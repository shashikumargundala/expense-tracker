package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.domain.category.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryRowMappingTest {

  @Test
  fun `categories read by header name, a blank Name being unkeyed`() {
    val columns = SheetValues.columns(listOf("Icon", "Name", "Color"))

    assertEquals(
      ParsedRow.Valid(Category("Food", "#F59E0B", "restaurant")),
      CategoryRowMapping.category(listOf("restaurant", " Food ", "#F59E0B"), columns),
    )
    assertEquals(ParsedRow.Valid(Category("Fun", "", "")), CategoryRowMapping.category(listOf(null, "Fun"), columns))
    assertEquals(ParsedRow.Unkeyed, CategoryRowMapping.category(listOf("x", "", "#000000"), columns))
  }

  @Test
  fun `cells keep Name, Color and Icon as text`() {
    assertEquals(
      mapOf("Name" to "'Pets", "Color" to "'#10B981", "Icon" to "'category"),
      CategoryRowMapping.cells(Category("Pets", "#10B981", "category")),
    )
  }

  @Test
  fun `a new Category takes the first unused swatch and the category icon`() {
    val existing = listOf(Category("Food", "#f59e0b", "restaurant"), Category("Bills", "#3B82F6", "receipt_long"))

    assertEquals(
      listOf(Category("Pets", "#10B981", "category"), Category("Gym", "#E11D48", "category")),
      CategoryRowMapping.newCategories(listOf("Pets", " Gym ", "pets"), existing),
    )
  }

  @Test
  fun `with all eight swatches taken a new Category is Food's colour`() {
    val existing = CategoryRowMapping.swatches.mapIndexed { i, color -> Category("C$i", color, "") }

    assertEquals("#F59E0B", CategoryRowMapping.newCategories(listOf("Pets"), existing).single().color)
  }
}
