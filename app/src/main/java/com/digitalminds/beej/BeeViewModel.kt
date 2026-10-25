package com.digitalminds.beej

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

data class BeeState(
    val page: String = "inicio", val selected: Int = 0, val mode: String = "practica",
    val answer: String = "", val revealed: Boolean = false, val checked: Boolean = false,
    val attempts: Int = 0, val wins: Int = 0, val streak: Int = 0,
    val currentDay: Int = 1, val mastered: Set<Int> = emptySet(),
    val wrong: Set<Int> = emptySet(), val lettersHidden: Boolean = false,
    val contestAlive: Boolean = true, val contestRound: Int = 1
)

class BeeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("bee_progress_v1", 0)
    private val _state = MutableStateFlow(BeeState(
        attempts = prefs.getInt("attempts",0), wins = prefs.getInt("wins",0),
        streak = prefs.getInt("streak",0), mastered = decode(prefs.getString("mastered", "")),
        wrong = decode(prefs.getString("wrong", "")), currentDay = daySinceStart()
    ))
    val state: StateFlow<BeeState> = _state
    private fun decode(raw: String?) = raw.orEmpty().split(',').mapNotNull { it.toIntOrNull() }.toSet()
    private fun daySinceStart(): Int {
        val today = LocalDate.now().toEpochDay()
        val first = prefs.getLong("first_day", -1)
        if (first < 0) { prefs.edit().putLong("first_day",today).apply(); return 1 }
        return (today-first+1).toInt().coerceIn(1,14)
    }
    private fun update(next: BeeState) { _state.value = next }
    fun go(page: String) = update(_state.value.copy(page = page, checked = false, answer = "", revealed = false))
    fun select(index: Int, page: String = "aprender") = update(_state.value.copy(page=page,selected=index.coerceIn(0,29),answer="",checked=false,revealed=false,mode=if(page=="reto") "practica" else _state.value.mode))
    fun reveal() = update(_state.value.copy(revealed = !_state.value.revealed))
    fun answer(value: String) { if (!_state.value.checked) update(_state.value.copy(answer=value.uppercase().filter { it in 'A'..'Z' })) }
    fun start(mode: String) {
        val first = if (mode == "repaso" && _state.value.wrong.isNotEmpty()) Words.all.indexOfFirst { it.id in _state.value.wrong } else 0
        update(_state.value.copy(page="reto", mode=mode, selected=first.coerceAtLeast(0),answer="",checked=false,revealed=false,contestAlive=true,contestRound=1))
    }
    fun check() {
        val s = _state.value
        if (s.checked || (s.mode == "concurso" && !s.contestAlive)) return
        val correct = Words.isCorrect(Words.all[s.selected], s.answer)
        val id = Words.all[s.selected].id
        val mastered = if (correct) s.mastered + id else s.mastered - id
        val wrong = if (correct) s.wrong - id else s.wrong + id
        val next = s.copy(checked=true, attempts=s.attempts+1, wins=s.wins+(if(correct)1 else 0),
            streak=if(correct) s.streak+1 else 0, mastered=mastered,wrong=wrong,
            contestAlive=s.mode!="concurso" || correct)
        update(next)
        prefs.edit().putInt("attempts",next.attempts).putInt("wins",next.wins)
            .putInt("streak",next.streak).putString("mastered",mastered.sorted().joinToString(","))
            .putString("wrong",wrong.sorted().joinToString(",")).apply()
    }
    fun next() {
        val s = _state.value
        val index = if (s.mode == "concurso") {
            // Deterministic unique rotation per competition, avoids repeating current word.
            (s.selected + 7) % 30
        } else if (s.mode == "repaso" && s.wrong.isNotEmpty()) {
            val ids = Words.all.filter { it.id in s.wrong }
            val currentPos = ids.indexOfFirst { it.id == Words.all[s.selected].id }
            Words.all.indexOf(ids[(currentPos+1).coerceAtLeast(0) % ids.size])
        } else (s.selected+1)%30
        update(s.copy(selected=index,checked=false,answer="",revealed=false,contestRound=s.contestRound+1))
    }
    fun toggleHide() = update(_state.value.copy(lettersHidden = !_state.value.lettersHidden))
}
