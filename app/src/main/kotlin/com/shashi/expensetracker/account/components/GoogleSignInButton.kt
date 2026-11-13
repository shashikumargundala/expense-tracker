package com.shashi.expensetracker.account.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shashi.expensetracker.R
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme

/** Minimum height, and the minimum touch target, of the Google button. */
val GoogleButtonMinHeight = 48.dp

/** Scale of the button while pressed, unless reduce motion is on. */
const val GoogleButtonPressedScale = 0.92f

/** One of Google's Sign in with Google button themes (branding guidelines, Android). */
@Immutable
data class GoogleButtonTheme(
  val fill: Color,
  val stroke: Color,
  val text: Color,
  @param:DrawableRes val logo: Int,
) {
  companion object {
    val Light =
      GoogleButtonTheme(
        fill = Color(0xFFFFFFFF),
        stroke = Color(0xFF747775),
        text = Color(0xFF1F1F1F),
        logo = R.drawable.ic_google_g_light,
      )
    val Dark =
      GoogleButtonTheme(
        fill = Color(0xFF131314),
        stroke = Color(0xFF8E918F),
        text = Color(0xFFE3E3E3),
        logo = R.drawable.ic_google_g_dark,
      )
  }
}

/** Google's light theme on a light app theme, its dark theme on a dark one. */
@Composable
fun googleButtonTheme(): GoogleButtonTheme =
  if (MaterialTheme.colorScheme.background.luminance() < 0.5f) GoogleButtonTheme.Dark
  else GoogleButtonTheme.Light

// Pill radius: a full pill at the 48dp minimum height; a wrapped label keeps the same corners.
private val GoogleButtonShape = RoundedCornerShape(24.dp)

// Google specifies Roboto Medium 14sp; the system sans on Android is Roboto.
private val GoogleButtonTextStyle =
  TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.25.sp,
  )

/**
 * Google's official Sign in with Google button in the pill shape: the "G" (20dp, 12dp from the
 * start, 10dp before the label) and [label], which wraps instead of truncating. The "G" is Google's
 * own raster from its branding assets, one per theme, because the current logo's blur and gradient
 * can't be expressed as a VectorDrawable. TalkBack reads "[label], button". It squashes to
 * [GoogleButtonPressedScale] on press unless reduce motion is on. Tagged `signin_continue_google`.
 */
@Composable
fun GoogleSignInButton(
  label: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  theme: GoogleButtonTheme = googleButtonTheme(),
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val reduceMotion = ExpenseTrackerTheme.reduceMotion
  val scale by
    animateFloatAsState(
      targetValue = if (pressed && !reduceMotion) GoogleButtonPressedScale else 1f,
      animationSpec = if (reduceMotion) snap() else spring(),
      label = "googleButtonScale",
    )
  // The hit area, semantics and tag stay full size; only the drawn pill squashes on press.
  Box(
    modifier =
      modifier
        .heightIn(min = GoogleButtonMinHeight)
        .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)
        .testTag("signin_continue_google"),
    contentAlignment = Alignment.Center,
    propagateMinConstraints = true,
  ) {
    GoogleButtonContent(label = label, theme = theme, scale = { scale }, interactionSource = interactionSource)
  }
}

@Composable
private fun GoogleButtonContent(
  label: String,
  theme: GoogleButtonTheme,
  scale: () -> Float,
  interactionSource: MutableInteractionSource,
) {
  Row(
    modifier =
      Modifier.graphicsLayer {
          scaleX = scale()
          scaleY = scale()
        }
        .clip(GoogleButtonShape)
        .background(theme.fill, GoogleButtonShape)
        .border(1.dp, theme.stroke, GoogleButtonShape)
        .indication(interactionSource, ripple(color = theme.text))
        .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.Center,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Image(painter = painterResource(theme.logo), contentDescription = null, modifier = Modifier.size(20.dp))
    Spacer(Modifier.width(10.dp))
    Text(
      text = label,
      style = GoogleButtonTextStyle,
      color = theme.text,
      textAlign = TextAlign.Center,
      modifier = Modifier.weight(1f, fill = false),
    )
  }
}
