package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals

class DicewarsScreenContractTest {

    @Test
    fun portHasSameNumberOfScreensAsOriginal() {
        assertEquals(10, DicewarsScreen.entries.size)
    }
}
