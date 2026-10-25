package com.shashi.expensetracker.ui.theme

import android.app.Application
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class GlassAndMotionTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun blurIsOnlySupportedFromApi31() {
    assertFalse(supportsBlur(24))
    assertFalse(supportsBlur(30))
    assertTrue(supportsBlur(31))
    assertTrue(supportsBlur(36))
  }

  @Test
  fun glassFallsBackToOpaqueSurfaceContainerOnApi30() {
    assertEquals(SurfaceContainer, glassFill(30, navStyle = false, LightExtendedColors, SurfaceContainer))
    assertEquals(SurfaceContainer, glassFill(30, navStyle = true, LightExtendedColors, SurfaceContainer))
    assertEquals(
      SurfaceContainerDark,
      glassFill(30, navStyle = false, DarkExtendedColors, SurfaceContainerDark),
    )
    assertEquals(1f, glassFill(30, navStyle = false, LightExtendedColors, SurfaceContainer).alpha)
  }

  @Test
  fun glassIsTranslucentOnApi31() {
    assertEquals(Glass, glassFill(31, navStyle = false, LightExtendedColors, SurfaceContainer))
    assertEquals(NavGlass, glassFill(31, navStyle = true, LightExtendedColors, SurfaceContainer))
    assertEquals(GlassDark, glassFill(31, navStyle = false, DarkExtendedColors, SurfaceContainerDark))
  }

  @Test
  fun designHexAlphaIsLastAndComposeAlphaIsFirst() {
    assertEquals(Color(0xB8FFFFFF), DesignTokens.parseDesignHex("#FFFFFFB8"))
    assertEquals(Color(0xFFFFF7F0), DesignTokens.parseDesignHex("#FFF7F0"))
    assertEquals(Color(0xB8FFFFFF), Glass)
    assertEquals(0xB8 / 255f, Glass.alpha, 0.001f)
    assertEquals(Color(0x732B1838), Scrim)
  }

  @Test
  fun reduceMotionIsTrueWhenAnimatorScaleIsZero() {
    setAnimatorScale(0f)
    assertTrue(renderReduceMotion())
  }

  @Test
  fun reduceMotionIsFalseWhenAnimatorScaleIsOne() {
    setAnimatorScale(1f)
    assertFalse(renderReduceMotion())
  }

  private fun setAnimatorScale(scale: Float) {
    val context = ApplicationProvider.getApplicationContext<Application>()
    Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, scale)
  }

  private fun renderReduceMotion(): Boolean {
    var reduceMotion: Boolean? = null
    composeRule.setContent { ExpenseTrackerTheme { reduceMotion = LocalReduceMotion.current } }
    composeRule.waitForIdle()
    return reduceMotion!!
  }
}
