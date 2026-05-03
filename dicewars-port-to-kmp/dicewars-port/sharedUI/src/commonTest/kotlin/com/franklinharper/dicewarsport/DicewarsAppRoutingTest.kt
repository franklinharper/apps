package com.franklinharper.dicewarsport

import kotlin.test.Test
import kotlin.test.assertEquals

class DicewarsAppRoutingTest {

    @Test
    fun composeRoutingCoversExactlyTheTenScreenStates() {
        assertEquals(DicewarsScreen.entries.toSet(), routedDicewarsScreens())
        assertEquals(10, routedDicewarsScreens().size)
    }
}
