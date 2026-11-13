package com.shashi.expensetracker.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shashi.expensetracker.R
import com.shashi.expensetracker.account.components.BrandMarkReferenceSize
import com.shashi.expensetracker.account.components.BrandRingsMark
import com.shashi.expensetracker.account.components.GoogleButtonMinHeight
import com.shashi.expensetracker.account.components.GoogleSignInButton
import com.shashi.expensetracker.account.components.SignInProgressRing
import com.shashi.expensetracker.ui.theme.ExpenseTrackerTheme
import com.shashi.expensetracker.ui.theme.Spacing

/** Sign-in wired to its ViewModel. Calls [onSignedIn] once the session is stored. */
@Composable
fun SignInRoute(
  contentPadding: PaddingValues,
  onSignedIn: () -> Unit,
  viewModel: SignInViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val currentOnSignedIn by rememberUpdatedState(onSignedIn)
  LaunchedEffect(uiState.signedIn) { if (uiState.signedIn) currentOnSignedIn() }
  SignInScreen(
    uiState = uiState,
    onContinueWithGoogle = viewModel::onContinueWithGoogle,
    contentPadding = contentPadding,
  )
}

/** Above this font scale, or below [CompactWidth], the brand mark shrinks to [CompactMarkSize]. */
private const val CompactFontScale = 1.3f
private val CompactWidth = 360.dp
private val CompactMarkSize = 80.dp

/** The app name's size at font scale 1, and how much of the font scale beyond 1 it follows. */
private val TitleSize = 28.dp
private const val TitleScaleFollow = 0.4f

/** The action area never grows wider than this on large screens. */
private val ActionMaxWidth = 400.dp

/**
 * Sign-in (DESIGN.md Sign-in screen), drawn over the shell's aurora: a centred brand block (the
 * rings mark and "Expense Tracker"), then the action area at the bottom inside [contentPadding].
 * The action area holds Google's Sign in with Google button, or while [SignInUiState.inProgress] a
 * gradient ring and "Setting up your Expense Sheet…" that TalkBack announces politely, then any
 * error line, then the privacy line. The column scrolls only when large text doesn't fit.
 */
@Composable
fun SignInScreen(
  uiState: SignInUiState,
  onContinueWithGoogle: () -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
) {
  BoxWithConstraints(modifier.fillMaxSize().testTag("screen_signin")) {
    val fontScale = LocalDensity.current.fontScale
    val compact = maxWidth < CompactWidth || fontScale > CompactFontScale
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .heightIn(min = maxHeight)
          .padding(contentPadding)
          .padding(horizontal = Spacing.marginMobile, vertical = Spacing.s4),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      BrandBlock(
        markSize = if (compact) CompactMarkSize else BrandMarkReferenceSize,
        modifier = Modifier.weight(1f).padding(vertical = Spacing.s5),
      )
      ActionArea(
        uiState = uiState,
        onContinueWithGoogle = onContinueWithGoogle,
        modifier = Modifier.widthIn(max = ActionMaxWidth).fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun BrandBlock(markSize: Dp, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(Spacing.s4, Alignment.CenterVertically),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    BrandRingsMark(Modifier.size(markSize))
    Text(
      text = stringResource(R.string.app_name),
      style =
        TextStyle(
          fontSize = titleFontSize(),
          lineHeight = 1.15.em,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.02).em,
        ),
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
    )
  }
}

/**
 * The app name at 28 at font scale 1, following only [TitleScaleFollow] of any larger scale, so it
 * grows less than body text (DESIGN.md non-linear display scaling) on every API level.
 */
@Composable
private fun titleFontSize() =
  with(LocalDensity.current) {
    val follow = 1f + (fontScale - 1f).coerceAtLeast(0f) * TitleScaleFollow
    (TitleSize * follow).toSp()
  }

@Composable
private fun ActionArea(uiState: SignInUiState, onContinueWithGoogle: () -> Unit, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(Spacing.s4),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    // No spacing here, so the empty status region adds no gap while idle.
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      if (!uiState.inProgress) {
        GoogleSignInButton(
          label = stringResource(R.string.signin_continue_google),
          onClick = onContinueWithGoogle,
          modifier = Modifier.fillMaxWidth(),
        )
      }
      SignInStatus(inProgress = uiState.inProgress)
    }
    uiState.error?.let { error ->
      Text(
        text = stringResource(error.messageRes()),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onErrorContainer,
        textAlign = TextAlign.Center,
        modifier =
          Modifier.testTag("signin_error")
            .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.medium)
            .padding(horizontal = Spacing.s4, vertical = Spacing.s2),
      )
    }
    PrivacyLine()
  }
}

/**
 * The polite live region, always composed (tag `signin_status`) and empty while idle. Signing in
 * fills it with the ring and "Setting up your Expense Sheet…" (tag `signin_progress`), so TalkBack
 * hears an existing live region change and announces the text without moving focus. It merges its
 * content into one node, so the text is read once.
 */
@Composable
private fun SignInStatus(inProgress: Boolean) {
  Box(
    modifier =
      Modifier.testTag("signin_status").semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    contentAlignment = Alignment.Center,
  ) {
    if (inProgress) SettingUp()
  }
}

/** Replaces the button while signing in. */
@Composable
private fun SettingUp() {
  Column(
    modifier = Modifier.heightIn(min = GoogleButtonMinHeight).testTag("signin_progress"),
    verticalArrangement = Arrangement.spacedBy(Spacing.s3),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    SignInProgressRing()
    Text(
      text = stringResource(R.string.signin_setting_up),
      style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(650)),
      color = MaterialTheme.colorScheme.onSurface,
      textAlign = TextAlign.Center,
      modifier = Modifier.testTag("signin_progress_text"),
    )
  }
}

/**
 * The lock and privacy line in meta. The lock sits inline in the text, so it stays beside the first
 * word when the line wraps. TalkBack reads only the sentence.
 */
@Composable
private fun PrivacyLine() {
  val privacy = stringResource(R.string.signin_privacy)
  val tint = MaterialTheme.colorScheme.onSurfaceVariant
  val text = buildAnnotatedString {
    appendInlineContent(LockInlineId, "lock")
    append(" ")
    append(privacy)
  }
  val inlineContent =
    mapOf(
      LockInlineId to
        InlineTextContent(Placeholder(1.3.em, 1.3.em, PlaceholderVerticalAlign.TextCenter)) {
          Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.fillMaxSize(),
          )
        }
    )
  Text(
    text = text,
    inlineContent = inlineContent,
    style = MaterialTheme.typography.bodySmall,
    color = tint,
    textAlign = TextAlign.Center,
    modifier = Modifier.testTag("signin_privacy").clearAndSetSemantics { this.text = AnnotatedString(privacy) },
  )
}

private const val LockInlineId = "lock"

private fun SignInError.messageRes(): Int =
  when (this) {
    SignInError.CouldNotSignIn -> R.string.signin_error_generic
    SignInError.NotConfigured -> R.string.signin_error_not_configured
  }

@Preview
@Composable
private fun SignInScreenPreview() {
  ExpenseTrackerTheme {
    SignInScreen(uiState = SignInUiState(error = SignInError.CouldNotSignIn), onContinueWithGoogle = {})
  }
}

@Preview
@Composable
private fun SignInScreenProgressDarkPreview() {
  ExpenseTrackerTheme(darkTheme = true) {
    SignInScreen(uiState = SignInUiState(inProgress = true), onContinueWithGoogle = {})
  }
}
