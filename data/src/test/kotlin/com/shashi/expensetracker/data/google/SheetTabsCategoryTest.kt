package com.shashi.expensetracker.data.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.SheetRow
import com.shashi.expensetracker.domain.account.SheetTab
import com.shashi.expensetracker.domain.account.TabReadResult
import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.category.Category
import com.squareup.moshi.Moshi
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The exact request bodies of the Category push's writes and deletes (AD-9), and the invalid keys
 * every read reports, through the real client.
 */
@RunWith(AndroidJUnit4::class)
class SheetTabsCategoryTest {

  private val server = MockWebServer()
  private val tokens = FakeTokenSource()
  private lateinit var gateway: SheetsExpenseSheetGateway

  @Before
  fun setUp() {
    server.start()
    val client = GoogleApiModule.googleOkHttpClient(tokens)
    val moshi = Moshi.Builder().build()
    val drive = GoogleApiModule.retrofit(server.url("/drive/v3/").toString(), client, moshi).create(DriveService::class.java)
    val sheets = GoogleApiModule.retrofit(server.url("/v4/").toString(), client, moshi).create(SheetsService::class.java)
    gateway = SheetsExpenseSheetGateway(drive, sheets)
  }

  @After fun tearDown() = server.close()

  private fun json(body: String, code: Int = 200) =
    MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

  @Test
  fun `writeCategory updates only Name, Color and Icon in place`() = runTest {
    server.enqueue(json("""{"range":"Categories!1:1","values":[["Icon","Notes","Name","Color"]]}"""))
    server.enqueue(json("{}"))

    assertEquals(RowWriteResult.Written(5), gateway.writeCategory("sheet-1", Category("Fun", "#10B981", "celebration"), 5))

    assertEquals("/v4/spreadsheets/sheet-1/values/Categories!1:1", server.takeRequest().url.encodedPath)
    val update = server.takeRequest()
    assertEquals("/v4/spreadsheets/sheet-1/values:batchUpdate", update.url.encodedPath)
    assertEquals(
      """{"valueInputOption":"USER_ENTERED","data":[""" +
        """{"range":"Categories!C5","values":[["'Fun"]]},{"range":"Categories!D5","values":[["'#10B981"]]},""" +
        """{"range":"Categories!A5","values":[["'celebration"]]}]}""",
      update.body!!.utf8(),
    )
  }

  @Test
  fun `writeCategory with no row appends Name, Color and Icon`() = runTest {
    server.enqueue(json("""{"range":"Categories!1:1","values":[["Name","Color","Icon"]]}"""))
    server.enqueue(json("""{"updates":{"updatedRange":"Categories!A7:C7"}}"""))

    assertEquals(RowWriteResult.Written(7), gateway.writeCategory("sheet-1", Category("Pets", "#A855F7", "pets"), null))

    server.takeRequest()
    val append = server.takeRequest()
    assertEquals("/v4/spreadsheets/sheet-1/values/Categories!A1:append", append.url.encodedPath)
    assertTrue(append.body!!.utf8(), append.body!!.utf8().contains("""[["'Pets","'#A855F7","'pets"]]"""))
  }

  @Test
  fun `writeExpenseCategories writes only the Category cell of each row, in one batch`() = runTest {
    server.enqueue(json("""{"range":"Expenses!1:1","values":[["Notes","ID","Date","Amount","Category","Description","Source","SMS ID"]]}"""))
    server.enqueue(json("{}"))

    assertEquals(RowWriteResult.Written(null), gateway.writeExpenseCategories("sheet-1", listOf(9, 3), "Food"))

    assertEquals("/v4/spreadsheets/sheet-1/values/Expenses!1:1", server.takeRequest().url.encodedPath)
    assertEquals(
      """{"valueInputOption":"USER_ENTERED","data":[""" +
        """{"range":"Expenses!E3","values":[["'Food"]]},{"range":"Expenses!E9","values":[["'Food"]]}]}""",
      server.takeRequest().body!!.utf8(),
    )
  }

  @Test
  fun `writeExpenseCategories sends nothing for no rows and fails without a usable header`() = runTest {
    assertEquals(RowWriteResult.Written(null), gateway.writeExpenseCategories("sheet-1", emptyList(), "Food"))
    assertEquals(0, server.requestCount)

    server.enqueue(json("""{"range":"Expenses!1:1","values":[["ID","Date","Amount"]]}"""))
    assertEquals(RowWriteResult.Failure(ExpenseSheetFailure.Unexpected), gateway.writeExpenseCategories("sheet-1", listOf(2), "Food"))
    assertEquals(1, server.requestCount)
  }

  @Test
  fun `deleteRows deletes a Budgets or Categories tab's rows bottom-up by its own tab ID`() = runTest {
    val tabs = """{"sheets":[{"properties":{"sheetId":0,"title":"Expenses"}},{"properties":{"sheetId":7,"title":"Categories"}},""" +
      """{"properties":{"sheetId":9,"title":"budgets"}}]}"""
    fun delete(tabId: Int, start: Int) =
      """{"deleteDimension":{"range":{"sheetId":$tabId,"dimension":"ROWS","startIndex":$start,"endIndex":${start + 1}}}}"""
    server.enqueue(json(tabs))
    server.enqueue(json("{}"))
    server.enqueue(json(tabs))
    server.enqueue(json("{}"))

    assertEquals(RowWriteResult.Written(null), gateway.deleteRows("sheet-1", SheetTab.Budgets, listOf(2, 4)))
    assertEquals(RowWriteResult.Written(null), gateway.deleteRows("sheet-1", SheetTab.Categories, listOf(3)))

    server.takeRequest()
    assertEquals("""{"requests":[${delete(9, 3)},${delete(9, 1)}]}""", server.takeRequest().body!!.utf8())
    server.takeRequest()
    assertEquals("""{"requests":[${delete(7, 2)}]}""", server.takeRequest().body!!.utf8())
  }

  @Test
  fun `every read reports the keys of its invalid rows`() = runTest {
    server.enqueue(
      json("""{"range":"Expenses!A1:D3","values":[["ID","Date","Amount","Category"],["a","2026-11-13",10,"Food"],["b","2026-11-13","abc","Food"]]}""")
    )
    server.enqueue(
      json("""{"range":"Budgets!A1:C4","values":[["Month","Category","Amount"],["2026-10","Food",0],["soon","Fun",1],["2026-11","Fun",5]]}""")
    )
    server.enqueue(json("""{"range":"Dismissed!A1:B2","values":[["SMS ID","Dismissed On"],["h:abc","later"]]}"""))
    server.enqueue(json("""{"range":"Categories!A1:C2","values":[["Name","Color","Icon"],["Food","#F59E0B","restaurant"]]}"""))

    val expenses = gateway.readExpenses("sheet-1") as TabReadResult.Rows
    assertEquals(setOf("b"), expenses.invalidKeys)
    assertEquals(listOf(SheetRow(3, "b")), expenses.invalidRows)
    assertEquals("Food", expenses.rows[1].value.category)
    assertEquals(
      TabReadResult.Rows(
        listOf(SheetRow(4, Budget(YearMonth.of(2026, 11), "Fun", 500))),
        skipped = 2,
        invalidKeys = setOf("2026-10|Food", "soon|Fun"),
        invalidRows = listOf(SheetRow(2, "2026-10|Food"), SheetRow(3, "soon|Fun")),
      ),
      gateway.readBudgets("sheet-1"),
    )
    assertEquals(listOf(SheetRow(2, "h:abc")), (gateway.readDismissals("sheet-1") as TabReadResult.Rows).invalidRows)
    assertEquals(emptySet<String>(), (gateway.readCategories("sheet-1") as TabReadResult.Rows).invalidKeys)
  }
}
