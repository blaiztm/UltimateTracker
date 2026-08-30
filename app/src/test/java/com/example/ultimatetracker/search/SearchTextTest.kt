package com.example.ultimatetracker.search

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchTextTest {
    @Test fun typoAndSpacingAreRecognized() {
        val q = normalizeSearchText("saikikuso")
        val c = normalizeSearchText("Saiki Kusuo")
        assertTrue(textMatchScore(q, c) >= .68)
    }

    @Test fun punctuationAndCaseNormalize() {
        assertEquals("saiki kusuo", normalizeSearchText(" SAIKI: Kusuo! ").normalized)
    }
}
