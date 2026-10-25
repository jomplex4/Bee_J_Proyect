package com.digitalminds.beej

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

enum class Screen {
    HOME, LEARN_LIST, LEARN_DETAIL, TRAIN_LIST, TRAIN_DETAIL, CONTEST, REVIEW_LIST, REVIEW_DETAIL, ALPHABET, METHOD, ABOUT
}

data class BeeState(
    val screen: Screen = Screen.HOME,
    val selectedWordId: Int = 1,
    val currentDay: Int = 1,
    val mastered: Set<Int> = emptySet(),
    val wrong: Set<Int> = emptySet(),
    val attempts: Int = 0,
    val wins: Int = 0,
    val streak: Int = 0,
    val wordSpeed: Float = 1.0f,
    val spellSpeed: Float = 1.0f,
    val useDoubleLetterCall: Boolean = false,
    val trainModeVoice: Boolean = true,
    val typedAttempt: String = "",
    val lastTypedLabel: String = "",
    val lastTypedExact: Boolean? = null,
    val lastVoiceTranscript: String = "",
    val lastVoiceResult: String = "",
    val contestRound: Int = 1,
    val contestVisibleWord: Boolean = false,
    val contestWordId: Int = 1,
    val contestMessage: String = "Presiona iniciar para mostrar la palabra",
    val contestPassedLast: Boolean? = null
)

class BeeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("beej_progress_v2", 0)

    private val _state = MutableStateFlow(
        BeeState(
            currentDay = computeDay(),
            mastered = decodeSet(prefs.getString("mastered", "")),
            wrong = decodeSet(prefs.getString("wrong", "")),
            attempts = prefs.getInt("attempts", 0),
            wins = prefs.getInt("wins", 0),
            streak = prefs.getInt("streak", 0),
            wordSpeed = prefs.getFloat("wordSpeed", 1.0f),
            spellSpeed = prefs.getFloat("spellSpeed", 1.0f),
            useDoubleLetterCall = prefs.getBoolean("doubleLetter", false)
        )
    )
    val state: StateFlow<BeeState> = _state

    fun go(screen: Screen) {
        _state.value = _state.value.copy(screen = screen, typedAttempt = "", lastTypedLabel = "", lastTypedExact = null, lastVoiceTranscript = "", lastVoiceResult = "")
    }

    fun openLearn(id: Int) = _state.update { it.copy(screen = Screen.LEARN_DETAIL, selectedWordId = id) }
    fun openTrain(id: Int) = _state.update { it.copy(screen = Screen.TRAIN_DETAIL, selectedWordId = id, typedAttempt = "", lastTypedLabel = "", lastTypedExact = null, lastVoiceTranscript = "", lastVoiceResult = "") }
    fun openReview(id: Int) = _state.update { it.copy(screen = Screen.REVIEW_DETAIL, selectedWordId = id, typedAttempt = "", lastTypedLabel = "", lastTypedExact = null, lastVoiceTranscript = "", lastVoiceResult = "") }

    fun nextWord() = _state.update { s ->
        val sequence = if (s.screen == Screen.REVIEW_DETAIL && s.wrong.isNotEmpty()) {
            Words.all.filter { it.id in s.wrong }.map { it.id }
        } else {
            Words.all.map { it.id }
        }
        val currentIndex = sequence.indexOf(s.selectedWordId).let { if (it == -1) 0 else it }
        val next = sequence[(currentIndex + 1) % sequence.size]
        s.copy(selectedWordId = next, typedAttempt = "", lastTypedLabel = "", lastTypedExact = null, lastVoiceTranscript = "", lastVoiceResult = "")
    }

    fun previousWord() = _state.update { s ->
        val sequence = if (s.screen == Screen.REVIEW_DETAIL && s.wrong.isNotEmpty()) {
            Words.all.filter { it.id in s.wrong }.map { it.id }
        } else {
            Words.all.map { it.id }
        }
        val currentIndex = sequence.indexOf(s.selectedWordId).let { if (it == -1) 0 else it }
        val prev = if (currentIndex == 0) sequence.last() else sequence[currentIndex - 1]
        s.copy(selectedWordId = prev, typedAttempt = "", lastTypedLabel = "", lastTypedExact = null, lastVoiceTranscript = "", lastVoiceResult = "")
    }

    fun setWordSpeed(value: Float) {
        _state.update { it.copy(wordSpeed = value) }
        prefs.edit().putFloat("wordSpeed", value).apply()
    }

    fun setSpellSpeed(value: Float) {
        _state.update { it.copy(spellSpeed = value) }
        prefs.edit().putFloat("spellSpeed", value).apply()
    }

    fun setDoubleLetterMode(enabled: Boolean) {
        _state.update { it.copy(useDoubleLetterCall = enabled) }
        prefs.edit().putBoolean("doubleLetter", enabled).apply()
    }

    fun setTrainModeVoice(enabled: Boolean) = _state.update { it.copy(trainModeVoice = enabled, lastVoiceTranscript = "", lastVoiceResult = "", typedAttempt = "", lastTypedLabel = "", lastTypedExact = null) }

    fun updateTypedAttempt(value: String) {
        val max = currentWord().letters.length
        _state.update { it.copy(typedAttempt = Words.normalizeLetters(value).take(max), lastTypedLabel = "", lastTypedExact = null) }
    }

    fun evaluateTypedAttempt() {
        val evaluation = SpellingEvaluator.evaluateTyped(currentWord(), _state.value.typedAttempt)
        val correct = evaluation.exact
        registerAttempt(correct, currentWord().id)
        _state.update { it.copy(lastTypedLabel = evaluation.label, lastTypedExact = correct) }
    }

    fun evaluateVoicePractice(transcript: String) {
        val letters = SpellingEvaluator.lettersFromSpeech(transcript)
        val evaluation = SpellingEvaluator.evaluateTyped(currentWord(), letters)
        registerAttempt(evaluation.exact, currentWord().id)
        val message = buildString {
            append(evaluation.label)
            append(" · ")
            append(letters.ifBlank { "sin letras reconocidas" })
        }
        _state.update { it.copy(lastVoiceTranscript = transcript, lastVoiceResult = message) }
    }

    fun startContest() {
        val firstId = selectContestWord(1)
        _state.update {
            it.copy(
                screen = Screen.CONTEST,
                contestRound = 1,
                contestWordId = firstId,
                contestVisibleWord = true,
                contestMessage = "Memoriza la palabra y prepárate para decirla",
                contestPassedLast = null,
                lastVoiceTranscript = "",
                lastVoiceResult = ""
            )
        }
    }

    fun hideContestWord() = _state.update { it.copy(contestVisibleWord = false, contestMessage = "Ahora di la palabra, deletrea y repite la palabra") }

    fun evaluateContestVoice(transcript: String) {
        val word = Words.byId(_state.value.contestWordId)
        val evaluation = SpellingEvaluator.evaluateContestVoice(word, transcript)
        registerAttempt(evaluation.exact, word.id)
        val nextState = if (evaluation.exact) {
            val nextRound = _state.value.contestRound + 1
            _state.value.copy(
                contestPassedLast = true,
                lastVoiceTranscript = transcript,
                lastVoiceResult = evaluation.message,
                contestMessage = "Correcto, pasa a la siguiente ronda",
                contestRound = nextRound,
                contestWordId = selectContestWord(nextRound),
                contestVisibleWord = true
            )
        } else {
            _state.value.copy(
                contestPassedLast = false,
                lastVoiceTranscript = transcript,
                lastVoiceResult = evaluation.message,
                contestMessage = "Ronda terminada",
                contestVisibleWord = false
            )
        }
        _state.value = nextState
    }

    private fun selectContestWord(round: Int): Int {
        return ((round * 7 - 1) % Words.all.size) + 1
    }

    private fun currentWord(): BeeWord = Words.byId(_state.value.selectedWordId)

    private fun registerAttempt(correct: Boolean, id: Int) {
        val state = _state.value
        val mastered = if (correct) state.mastered + id else state.mastered
        val wrong = if (correct) state.wrong - id else state.wrong + id
        val updated = state.copy(
            attempts = state.attempts + 1,
            wins = state.wins + if (correct) 1 else 0,
            streak = if (correct) state.streak + 1 else 0,
            mastered = mastered,
            wrong = wrong
        )
        _state.value = updated
        prefs.edit()
            .putInt("attempts", updated.attempts)
            .putInt("wins", updated.wins)
            .putInt("streak", updated.streak)
            .putString("mastered", mastered.sorted().joinToString(","))
            .putString("wrong", wrong.sorted().joinToString(","))
            .apply()
    }

    private fun computeDay(): Int {
        val today = LocalDate.now().toEpochDay()
        val first = prefs.getLong("firstDay", -1)
        if (first == -1L) {
            prefs.edit().putLong("firstDay", today).apply()
            return 1
        }
        return (today - first + 1).toInt().coerceIn(1, 14)
    }

    private fun decodeSet(raw: String?): Set<Int> = raw.orEmpty().split(',').mapNotNull { it.toIntOrNull() }.toSet()

    private inline fun MutableStateFlow<BeeState>.update(block: (BeeState) -> BeeState) {
        value = block(value)
    }
}
