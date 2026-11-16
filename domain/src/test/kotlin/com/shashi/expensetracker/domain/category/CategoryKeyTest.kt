package com.shashi.expensetracker.domain.category

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CategoryKeyTest {

  @Test
  fun `a key is trimmed with ASCII letters folded`() {
    assertEquals("food", CategoryKey.of(" Food "))
    assertEquals(CategoryKey.of("FOOD"), CategoryKey.of("food"))
  }

  @Test
  fun `non-ASCII letters are not folded, as SQLite NOCASE does not fold them`() {
    assertEquals("cafÉ", CategoryKey.of("CAFÉ"))
    assertNotEquals(CategoryKey.of("Café"), CategoryKey.of("CAFÉ"))
  }
}
