package com.example.bookapp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentValidatorTest {
    @Test fun `valid content with stable uids passes`() {
        val json = """[{"uid":"f1","title":"اصفهان","taziehs":[{"uid":"t1","title":"عاشورا","roles":[{"uid":"r1","title":"امام","sections":[{"uid":"s1","title":"ورود","content":"متن"}]}]}]}]"""
        assertTrue(ContentValidator.validate(json).isEmpty())
    }

    @Test fun `duplicate section uid is rejected`() {
        val json = """[{"uid":"f1","title":"اصفهان","taziehs":[{"uid":"t1","title":"عاشورا","roles":[{"uid":"r1","title":"امام","sections":[{"uid":"s1","title":"A","content":"متن"},{"uid":"s1","title":"B","content":"متن"}]}]}]}]"""
        assertFalse(ContentValidator.validate(json).isEmpty())
    }
}
