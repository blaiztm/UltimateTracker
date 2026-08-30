package com.example.ultimatetracker.viewmodel

import com.example.ultimatetracker.data.model.BuiltInMediaTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaFormStateTest {
    @Test fun blankTitleIsRejected() {
        assertEquals(FormValidationError.TITLE, MediaFormState(length = "120").validationError())
    }

    @Test fun invalidEpisodeCountIsRejected() {
        val form = MediaFormState(title = "Arcane", type = BuiltInMediaTypes.SERIES, length = "0")
        assertEquals(FormValidationError.EPISODES, form.validationError())
    }

    @Test fun validFormPasses() {
        assertNull(MediaFormState(title = "Dune", length = "155").validationError())
    }
}
