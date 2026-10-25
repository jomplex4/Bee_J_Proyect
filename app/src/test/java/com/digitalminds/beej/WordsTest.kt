package com.digitalminds.beej

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordsTest {
    @Test
    fun wordsListHas30Entries() {
        assertEquals(30, Words.all.size)
    }

    @Test
    fun carefulIsIncluded() {
        assertTrue(Words.all.any { it.value == "CAREFUL" })
    }

    @Test
    fun normalizeLettersKeepsOnlyAlphabet() {
        assertEquals("CAREFUL", Words.normalizeLetters("c-a r e!f u l"))
    }
}
