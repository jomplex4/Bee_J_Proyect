package com.digitalminds.beej

import org.junit.Assert.*
import org.junit.Test

class WordsTest {
    @Test fun onlyThirtyUniqueWords() {
        assertEquals(30,Words.all.size)
        assertEquals(30,Words.all.map { it.value }.distinct().size)
        assertEquals((1..30).toList(), Words.all.map { it.id })
    }
    @Test fun carefulCorrected() {
        assertEquals("CAREFUL",Words.all[17].value)
        assertFalse(Words.all.any { it.value == "CARIFUL" })
    }
    @Test fun spellingIsStrictInOrder() {
        assertTrue(Words.isCorrect(Words.all[0],"s i n g e r"))
        assertTrue(Words.isCorrect(Words.all[0],"SINGER"))
        assertFalse(Words.isCorrect(Words.all[0],"SIGNER"))
    }
    @Test fun trainingGroupsStayInCorpus() {
        for(day in 1..14) assertTrue(Words.dayGroup(day).all { it in Words.all })
        assertEquals(30,Words.dayGroup(14).size)
    }
}
