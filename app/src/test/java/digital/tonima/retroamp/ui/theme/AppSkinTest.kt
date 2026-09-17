package digital.tonima.retroamp.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSkinTest {

    @Test
    fun `fromName resolves the 8-bit skin by its stored name`() {
        assertEquals(AppSkin.EightBit, AppSkin.fromName(AppSkin.EightBit.name))
    }

    @Test
    fun `fromName resolves the winamp skin by its stored name`() {
        assertEquals(AppSkin.Winamp, AppSkin.fromName(AppSkin.Winamp.name))
    }

    @Test
    fun `fromName falls back to winamp for an unknown or missing name`() {
        assertEquals(AppSkin.Winamp, AppSkin.fromName("does-not-exist"))
        assertEquals(AppSkin.Winamp, AppSkin.fromName(""))
    }
}
