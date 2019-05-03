package industries.goodteam.gambit

import junit.framework.Assert.assertEquals
import org.junit.Test

class EventTest {

    @Test
    fun constructor_works() {
        val event = StartCombat(3)
        assertEquals(event.number, 3)
    }
}