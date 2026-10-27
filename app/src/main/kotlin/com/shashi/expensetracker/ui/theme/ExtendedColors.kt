package com.shashi.expensetracker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Jelly Pop tokens that have no M3 `ColorScheme` role (DESIGN.md Colors). */
@Immutable
data class ExtendedColors(
  val gradientStart: Color,
  val gradientEnd: Color,
  /** Content drawn on the gradient (active nav item); white in both modes. */
  val onGradient: Color,
  val glass: Color,
  val glassEdge: Color,
  val navGlass: Color,
  val aurora1: Color,
  val aurora2: Color,
  val aurora3: Color,
  val warning: Color,
  val warningContainer: Color,
  val pendingSync: Color,
  val pendingSyncContainer: Color,
  val inbox: Color,
  val inboxContainer: Color,
  val categoryFood: Color,
  val categoryFoodTint: Color,
  val categoryTransport: Color,
  val categoryTransportTint: Color,
  val categoryShopping: Color,
  val categoryShoppingTint: Color,
  val categoryBills: Color,
  val categoryBillsTint: Color,
  val categoryFun: Color,
  val categoryFunTint: Color,
  val edge: Color,
  val scrim: Color,
)

val LightExtendedColors =
  ExtendedColors(
    gradientStart = GradientStart,
    gradientEnd = GradientEnd,
    onGradient = OnGradient,
    glass = Glass,
    glassEdge = GlassEdge,
    navGlass = NavGlass,
    aurora1 = Aurora1,
    aurora2 = Aurora2,
    aurora3 = Aurora3,
    warning = Warning,
    warningContainer = WarningContainer,
    pendingSync = PendingSync,
    pendingSyncContainer = PendingSyncContainer,
    inbox = Inbox,
    inboxContainer = InboxContainer,
    categoryFood = CategoryFood,
    categoryFoodTint = CategoryFoodTint,
    categoryTransport = CategoryTransport,
    categoryTransportTint = CategoryTransportTint,
    categoryShopping = CategoryShopping,
    categoryShoppingTint = CategoryShoppingTint,
    categoryBills = CategoryBills,
    categoryBillsTint = CategoryBillsTint,
    categoryFun = CategoryFun,
    categoryFunTint = CategoryFunTint,
    edge = Edge,
    scrim = Scrim,
  )

val DarkExtendedColors =
  ExtendedColors(
    gradientStart = GradientStartDark,
    gradientEnd = GradientEndDark,
    onGradient = OnGradientDark,
    glass = GlassDark,
    glassEdge = GlassEdgeDark,
    navGlass = NavGlassDark,
    aurora1 = Aurora1Dark,
    aurora2 = Aurora2Dark,
    aurora3 = Aurora3Dark,
    warning = WarningDark,
    warningContainer = WarningContainerDark,
    pendingSync = PendingSyncDark,
    pendingSyncContainer = PendingSyncContainerDark,
    inbox = InboxDark,
    inboxContainer = InboxContainerDark,
    categoryFood = CategoryFoodDark,
    categoryFoodTint = CategoryFoodTintDark,
    categoryTransport = CategoryTransportDark,
    categoryTransportTint = CategoryTransportTintDark,
    categoryShopping = CategoryShoppingDark,
    categoryShoppingTint = CategoryShoppingTintDark,
    categoryBills = CategoryBillsDark,
    categoryBillsTint = CategoryBillsTintDark,
    categoryFun = CategoryFunDark,
    categoryFunTint = CategoryFunTintDark,
    edge = EdgeDark,
    scrim = ScrimDark,
  )

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
