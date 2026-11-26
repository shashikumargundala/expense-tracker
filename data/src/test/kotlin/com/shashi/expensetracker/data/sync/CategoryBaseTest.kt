package com.shashi.expensetracker.data.sync

import com.shashi.expensetracker.domain.category.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** [CategoryBase]'s codec and its comparison with a Sheet row (AD-4, AD-9). */
class CategoryBaseTest {

  private val fun_ = Category("Fun", "#A855F7", "celebration")

  @Test
  fun `a base round-trips Name, Color and Icon`() {
    val decoded = checkNotNull(CategoryBase.decode(CategoryBase.encode(fun_)))

    assertEquals(CategoryBase("Fun", "#A855F7", "celebration"), decoded)
    assertTrue(decoded.matches(fun_))
  }

  @Test
  fun `a null base is absent and an unreadable one throws IllegalArgumentException`() {
    assertNull(CategoryBase.decode(null))
    assertThrows(IllegalArgumentException::class.java) { CategoryBase.decode("not json") }
    assertThrows(IllegalArgumentException::class.java) { CategoryBase.decode("{}") }
  }

  @Test
  fun `the name compares by CategoryKey, Color and Icon exactly`() {
    val base = checkNotNull(CategoryBase.decode(CategoryBase.encode(fun_)))

    assertTrue(base.matches(fun_.copy(name = " FUN ")))
    assertFalse(base.matches(fun_.copy(color = "#000000")))
    assertFalse(base.matches(fun_.copy(color = "#a855f7")))
    assertFalse(base.matches(fun_.copy(icon = "")))
    assertFalse(base.matches(fun_.copy(name = "Food")))
  }
}
