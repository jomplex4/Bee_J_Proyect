package com.digitalminds.beej

import kotlin.math.min

data class TypedEvaluation(
    val normalizedAttempt: String,
    val exact: Boolean,
    val score: Int,
    val label: String,
    val distance: Int
)

data class VoiceContestEvaluation(
    val exact: Boolean,
    val heardWordAtStart: Boolean,
    val heardWordAtEnd: Boolean,
    val spelledLetters: String,
    val transcript: String,
    val message: String
)

object SpellingEvaluator {
    private val singleTokenMap = mapOf(
        "a" to 'A', "ay" to 'A', "hey" to 'A',
        "b" to 'B', "bee" to 'B', "be" to 'B',
        "c" to 'C', "see" to 'C', "sea" to 'C',
        "d" to 'D', "dee" to 'D',
        "e" to 'E', "ee" to 'E',
        "f" to 'F', "ef" to 'F', "eff" to 'F',
        "g" to 'G', "gee" to 'G',
        "h" to 'H', "aitch" to 'H', "haitch" to 'H',
        "i" to 'I', "eye" to 'I',
        "j" to 'J', "jay" to 'J',
        "k" to 'K', "kay" to 'K',
        "l" to 'L', "el" to 'L', "ell" to 'L',
        "m" to 'M', "em" to 'M',
        "n" to 'N', "en" to 'N',
        "o" to 'O', "oh" to 'O',
        "p" to 'P', "pee" to 'P',
        "q" to 'Q', "cue" to 'Q', "queue" to 'Q',
        "r" to 'R', "ar" to 'R', "are" to 'R',
        "s" to 'S', "ess" to 'S',
        "t" to 'T', "tee" to 'T',
        "u" to 'U', "you" to 'U',
        "v" to 'V', "vee" to 'V',
        "w" to 'W',
        "x" to 'X', "ex" to 'X',
        "y" to 'Y', "why" to 'Y',
        "z" to 'Z', "zee" to 'Z', "zed" to 'Z'
    )

    fun evaluateTyped(word: BeeWord, attempt: String): TypedEvaluation {
        val normalized = Words.normalizeLetters(attempt)
        val target = word.letters
        val distance = levenshtein(normalized, target)
        val exact = normalized == target
        val score = when {
            exact -> 100
            distance == 1 -> 85
            distance == 2 -> 70
            distance <= 3 -> 55
            else -> 25
        }
        val label = when {
            exact -> "Perfecto"
            distance == 1 -> "Casi bien"
            distance == 2 -> "Bien"
            else -> "Reforzar"
        }
        return TypedEvaluation(normalized, exact, score, label, distance)
    }

    fun lettersFromSpeech(transcript: String): String {
        val tokens = transcript.lowercase()
            .replace("-", " ")
            .replace(Regex("[^a-z ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

        val out = StringBuilder()
        var i = 0
        while (i < tokens.size) {
            val current = tokens[i]
            if (current == "double" && i + 1 < tokens.size) {
                val pairLetter = parseLetter(tokens, i + 1)
                if (pairLetter != null) {
                    out.append(pairLetter.first)
                    out.append(pairLetter.first)
                    i += pairLetter.second + 1
                    continue
                }
            }
            val parsed = parseLetter(tokens, i)
            if (parsed != null) {
                out.append(parsed.first)
                i += parsed.second
            } else {
                i += 1
            }
        }
        return out.toString()
    }

    fun evaluateContestVoice(word: BeeWord, transcript: String): VoiceContestEvaluation {
        val cleanedTokens = transcript.lowercase()
            .replace(Regex("[^a-z ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        val targetWord = word.value.lowercase()
        val heardWordAtStart = cleanedTokens.firstOrNull() == targetWord
        val heardWordAtEnd = cleanedTokens.lastOrNull() == targetWord
        val spelledLetters = lettersFromSpeech(transcript)
        val exact = heardWordAtStart && heardWordAtEnd && spelledLetters == word.letters
        val message = when {
            exact -> "Formato oficial correcto"
            !heardWordAtStart && !heardWordAtEnd -> "Debes decir la palabra antes y después del deletreo"
            !heardWordAtStart -> "Falta decir la palabra al inicio"
            !heardWordAtEnd -> "Falta decir la palabra al final"
            else -> "El deletreo no coincide"
        }
        return VoiceContestEvaluation(exact, heardWordAtStart, heardWordAtEnd, spelledLetters, transcript, message)
    }

    private fun parseLetter(tokens: List<String>, index: Int): Pair<Char, Int>? {
        if (index >= tokens.size) return null
        if (index + 1 < tokens.size && tokens[index] == "double" && tokens[index + 1] == "you") {
            return 'W' to 2
        }
        if (tokens[index] == "doubleyou") return 'W' to 1
        if (index + 1 < tokens.size && tokens[index] == "double" && tokens[index + 1] == "u") {
            return 'W' to 2
        }
        val single = singleTokenMap[tokens[index]]
        if (single != null) return single to 1
        if (index + 1 < tokens.size) {
            val joined = tokens[index] + tokens[index + 1]
            val joinedLetter = singleTokenMap[joined]
            if (joinedLetter != null) return joinedLetter to 2
        }
        return null
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }
}
