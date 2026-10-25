package com.digitalminds.beej

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpellingEvaluatorTest {
    private val careful = Words.byId(18)

    @Test
    fun typedEvaluationDetectsExactAnswer() {
        val evaluation = SpellingEvaluator.evaluateTyped(careful, "careful")
        assertTrue(evaluation.exact)
        assertEquals("Perfecto", evaluation.label)
    }

    @Test
    fun lettersFromSpeechParsesDoubleLetter() {
        val letters = SpellingEvaluator.lettersFromSpeech("coffee c o double f e e")
        assertEquals("COFFEE", letters)
    }

    @Test
    fun contestVoiceRequiresWordAtStartAndEnd() {
        val result = SpellingEvaluator.evaluateContestVoice(careful, "careful c a r e f u l careful")
        assertTrue(result.exact)
    }

    @Test
    fun contestVoiceFailsWhenFinalWordIsMissing() {
        val result = SpellingEvaluator.evaluateContestVoice(careful, "careful c a r e f u l")
        assertFalse(result.exact)
    }
}
