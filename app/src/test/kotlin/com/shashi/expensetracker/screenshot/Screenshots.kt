package com.shashi.expensetracker.screenshot

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage

/** Where screenshot baselines are committed, relative to the `:app` module directory. */
const val SCREENSHOT_DIR = "src/test/screenshots"

/**
 * Waits for the UI to go idle, then captures the whole compose root as `<name>.png` in
 * [SCREENSHOT_DIR].
 *
 * Name baselines `<screen>_<variant>.png`, for example `shell_home_light`. Plain `test` runs only
 * render; `recordRoborazziDebug` writes the baselines and `verifyRoborazziDebug` compares against
 * them (a diff writes a `_compare` image under `app/build/outputs/roborazzi/`).
 *
 * To add screenshots for a later screen:
 * 1. Add `<Screen>ScreenshotTest` in this package, annotated `@RunWith(AndroidJUnit4::class)`,
 *    `@GraphicsMode(GraphicsMode.Mode.NATIVE)` and `@Config(qualifiers = "w360dp-h800dp")`.
 * 2. Make it a `@HiltAndroidTest` with `application = HiltTestApplication::class` in its `@Config`
 *    and a `HiltAndroidRule` at rule order 0. Launch the real `MainActivity` with
 *    `createAndroidComposeRule<MainActivity>()` at order 1 (`FakeAccountModule` gives a signed-in
 *    session, so it opens Home), navigate to the screen and wait for its `screen_*` tag.
 * 3. Get variants only from `@Config`: `+night` for dark, `fontScale = 1.75f` for large text and
 *    `w600dp-h900dp` for the rail. Never add test-only code to the app.
 * 4. Call `composeRule.captureScreen("<screen>_<variant>")`, run `./gradlew recordRoborazziDebug`
 *    on a passing build, check each new PNG by eye and commit it.
 */
fun ComposeTestRule.captureScreen(name: String) {
  waitForIdle()
  onRoot().captureRoboImage("$SCREENSHOT_DIR/$name.png")
}
