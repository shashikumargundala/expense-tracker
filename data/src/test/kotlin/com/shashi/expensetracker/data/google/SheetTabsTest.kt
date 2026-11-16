package com.shashi.expensetracker.data.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.data.sampleExpense
import com.shashi.expensetracker.domain.account.AccessResult
import com.shashi.expensetracker.domain.account.ConsentRequest
import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.account.SheetRow
import com.shashi.expensetracker.domain.account.TabReadResult
import com.shashi.expensetracker.domain.budget.Budget
import com.shashi.expensetracker.domain.category.Category
import com.shashi.expensetracker.domain.inbox.Dismissal
import com.shashi.expensetracker.domain.settings.Setting
import com.squareup.moshi.Moshi
import java.time.LocalDateTime
import java.time.YearMonth
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The gateway's tab reads and row writes against `mockwebserver3`, through the real client. */
@RunWith(AndroidJUnit4::class)
class SheetTabsTest {

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

  private fun sample(name: String): String =
    checkNotNull(javaClass.getResource("/google/$name")) { "Missing sample $name" }.readText()

  private val expensesHeader = """["ID","Date","Amount","Category","Description","Source","SMS ID"]"""

  private fun RecordedRequest.assertReadOptions() {
    assertEquals("GET", method)
    assertEquals("UNFORMATTED_VALUE", url.queryParameter("valueRenderOption"))
    assertEquals("SERIAL_NUMBER", url.queryParameter("dateTimeRenderOption"))
  }

  @Test
  fun `readExpenses reads the whole tab unformatted with serial dates`() = runTest {
    server.enqueue(json(sample("sheets-value-range.json")))

    val result = gateway.readExpenses("sheet-1") as TabReadResult.Rows

    val request = server.takeRequest()
    assertEquals("/v4/spreadsheets/sheet-1/values/Expenses", request.url.encodedPath)
    request.assertReadOptions()
    val row = result.rows.single()
    assertEquals(2, row.row)
    assertEquals("0f8fad5b-d9cb-469f-a165-70867728950e", row.value.id)
    assertEquals(25_075L, row.value.amountPaise)
    assertEquals(0, result.skipped)
  }

  @Test
  fun `readExpenses keeps sheet row numbers past blank rows and counts invalid rows but not blank IDs`() = runTest {
    server.enqueue(
      json(
        """{"range":"Expenses!A1:G6","values":[$expensesHeader,
          ["a",46339.0,10,"Food"],[],["","2026-11-13",5,"Food"],["b","2026-11-13 09:30","₹1,200","Bills"],
          ["c","2026-11-13","abc","Food"]]}"""
      )
    )

    val result = gateway.readExpenses("sheet-1") as TabReadResult.Rows

    assertEquals(listOf(2 to "a", 5 to "b"), result.rows.map { it.row to it.value.id })
    assertEquals(120_000L, result.rows[1].value.amountPaise)
    assertEquals(1, result.skipped)
  }

  @Test
  fun `a read whose tab still lacks a required header is an Unexpected failure`() = runTest {
    server.enqueue(json("""{"range":"Expenses!A1:B2","values":[["ID","Date"],["a",46339.0]]}"""))

    assertEquals(TabReadResult.Failure(ExpenseSheetFailure.Unexpected), gateway.readExpenses("sheet-1"))
  }

  @Test
  fun `Budgets, Dismissed and Settings read by header name and count invalid rows`() = runTest {
    server.enqueue(
      json("""{"range":"Budgets!A1:D4","values":[["Notes","Month","Category","Amount"],["x","2026-10","Food","5,000"],["","","Food",100],["","2026-11","Bills",250.5]]}""")
    )
    server.enqueue(json("""{"range":"Dismissed!A1:B3","values":[["SMS ID","Dismissed On"],["h:abc",46339.0],["h:def","later"]]}"""))
    server.enqueue(json("""{"range":"Settings!A1:B3","values":[["Key","Value"],["Currency","INR"],["Name"]]}"""))

    val budgets = gateway.readBudgets("sheet-1")
    val dismissals = gateway.readDismissals("sheet-1")
    val settings = gateway.readSettings("sheet-1")

    assertEquals(
      TabReadResult.Rows(
        listOf(SheetRow(2, Budget(YearMonth.of(2026, 10), "Food", 500_000)), SheetRow(4, Budget(YearMonth.of(2026, 11), "Bills", 25_050))),
        skipped = 1,
      ),
      budgets,
    )
    assertEquals(TabReadResult.Rows(listOf(SheetRow(2, Dismissal("h:abc", LocalDateTime.of(2026, 11, 13, 0, 0)))), skipped = 1), dismissals)
    assertEquals(TabReadResult.Rows(listOf(SheetRow(2, Setting("Currency", "INR")), SheetRow(3, Setting("Name", "")))), settings)
    listOf("Budgets", "Dismissed", "Settings").forEach { tab ->
      val request = server.takeRequest()
      assertEquals("/v4/spreadsheets/sheet-1/values/$tab", request.url.encodedPath)
      request.assertReadOptions()
    }
  }

  @Test
  fun `readCategories reads Name, Color and Icon`() = runTest {
    server.enqueue(
      json("""{"range":"Categories!A1:C3","values":[["Name","Color","Icon"],["Food","#F59E0B","restaurant"],["Fun"]]}""")
    )

    val result = gateway.readCategories("sheet-1")

    assertEquals(
      TabReadResult.Rows(
        listOf(SheetRow(2, Category("Food", "#F59E0B", "restaurant")), SheetRow(3, Category("Fun", "", "")))
      ),
      result,
    )
    val request = server.takeRequest()
    assertEquals("/v4/spreadsheets/sheet-1/values/Categories", request.url.encodedPath)
    request.assertReadOptions()
  }

  @Test
  fun `reads map a deleted sheet, network and consent failures`() = runTest {
    server.enqueue(json("""{"error":{"code":404}}""", code = 404))
    assertEquals(TabReadResult.SheetMissing, gateway.readExpenses("gone"))

    server.enqueue(json("{}", code = 500))
    assertEquals(TabReadResult.Failure(ExpenseSheetFailure.Unexpected), gateway.readCategories("sheet-1"))

    val request = object : ConsentRequest {}
    tokens.denied = AccessResult.NeedsUserConsent(request)
    assertEquals(TabReadResult.NeedsConsent(request), gateway.readExpenses("sheet-1"))

    tokens.denied = null
    server.close()
    assertEquals(TabReadResult.Failure(ExpenseSheetFailure.Network), gateway.readExpenses("sheet-1"))
  }

  @Test
  fun `a new Expense is appended USER_ENTERED in header order with Source App`() = runTest {
    server.enqueue(json("""{"range":"Expenses!1:1","values":[["Notes","ID","Date","Amount","Category","Description","Source","SMS ID"]]}"""))
    server.enqueue(json("""{"updates":{"updatedRange":"Expenses!A7:H7"}}"""))

    val result = gateway.writeExpense("sheet-1", sampleExpense(id = "id-1"), existingRow = null)

    assertEquals(RowWriteResult.Written(7), result)
    val header = server.takeRequest()
    assertEquals("/v4/spreadsheets/sheet-1/values/Expenses!1:1", header.url.encodedPath)
    header.assertReadOptions()
    val append = server.takeRequest()
    assertEquals("POST", append.method)
    assertEquals("/v4/spreadsheets/sheet-1/values/Expenses!A1:append", append.url.encodedPath)
    assertEquals("USER_ENTERED", append.url.queryParameter("valueInputOption"))
    assertEquals("INSERT_ROWS", append.url.queryParameter("insertDataOption"))
    assertTrue(
      append.body!!.utf8(),
      append.body!!.utf8().contains("""[["","'id-1","2026-11-13 09:30","120.00","'Food","'Chai","App",""]]"""),
    )
  }

  @Test
  fun `an existing row is updated in place, mapped cells only, without Source`() = runTest {
    server.enqueue(json("""{"range":"Expenses!1:1","values":[["ID","Date","Amount","Category","Notes","Description","Source","SMS ID"]]}"""))
    server.enqueue(json("{}"))
    val expense = sampleExpense(id = "id-1", date = LocalDateTime.of(2026, 11, 3, 7, 5))

    assertEquals(RowWriteResult.Written(4), gateway.writeExpense("sheet-1", expense, existingRow = 4))

    server.takeRequest()
    val update = server.takeRequest()
    assertEquals("/v4/spreadsheets/sheet-1/values:batchUpdate", update.url.encodedPath)
    val body = update.body!!.utf8()
    assertTrue(body, body.contains("\"valueInputOption\":\"USER_ENTERED\""))
    listOf("Expenses!A4", "Expenses!B4", "Expenses!C4", "Expenses!D4", "Expenses!F4", "Expenses!H4").forEach {
      assertTrue(it, body.contains("\"$it\""))
    }
    assertFalse("Notes column untouched", body.contains("Expenses!E4"))
    assertFalse("Source untouched", body.contains("Expenses!G4"))
    assertTrue(body, body.contains("2026-11-03 07:05"))
  }

  @Test
  fun `a write fails without a usable header or when the sheet is gone`() = runTest {
    server.enqueue(json("""{"range":"Expenses!1:1","values":[["Date","Amount"]]}"""))
    assertEquals(RowWriteResult.Failure(ExpenseSheetFailure.Unexpected), gateway.writeExpense("sheet-1", sampleExpense(), null))
    assertEquals(1, server.requestCount)

    server.enqueue(json("""{"error":{"code":404}}""", code = 404))
    assertEquals(RowWriteResult.SheetMissing, gateway.writeExpense("gone", sampleExpense(), null))

    server.enqueue(json("""{"range":"Expenses!1:1","values":[$expensesHeader]}"""))
    server.enqueue(json("{}", code = 503))
    assertEquals(RowWriteResult.Failure(ExpenseSheetFailure.Unexpected), gateway.writeExpense("sheet-1", sampleExpense(), null))
  }

  @Test
  fun `an append whose reply has no range is written with an unknown row`() = runTest {
    server.enqueue(json("""{"range":"Expenses!1:1","values":[$expensesHeader]}"""))
    server.enqueue(json("{}"))

    assertEquals(RowWriteResult.Written(null), gateway.writeExpense("sheet-1", sampleExpense(), null))
  }

  @Test
  fun `appendCategories appends Name, Color and Icon under the current header, as text`() = runTest {
    server.enqueue(json("""{"range":"Categories!1:1","values":[["Icon","Notes","Name","Color"]]}"""))
    server.enqueue(json("""{"updates":{"updatedRange":"Categories!A7:D8"}}"""))

    val result =
      gateway.appendCategories("sheet-1", listOf(Category("Pets", "#10B981", "category"), Category("Gym", "#E11D48", "category")))

    assertEquals(RowWriteResult.Written(7), result)
    assertEquals("/v4/spreadsheets/sheet-1/values/Categories!1:1", server.takeRequest().url.encodedPath)
    val append = server.takeRequest()
    assertEquals("/v4/spreadsheets/sheet-1/values/Categories!A1:append", append.url.encodedPath)
    assertEquals("USER_ENTERED", append.url.queryParameter("valueInputOption"))
    val body = append.body!!.utf8()
    assertTrue(body, body.contains("""[["'category","","'Pets","'#10B981"],["'category","","'Gym","'#E11D48"]]"""))
  }

  @Test
  fun `appendCategories fails without a Name header and sends nothing for no Categories`() = runTest {
    assertEquals(RowWriteResult.Written(null), gateway.appendCategories("sheet-1", emptyList()))
    assertEquals(0, server.requestCount)

    server.enqueue(json("""{"range":"Categories!1:1","values":[["Color"]]}"""))
    assertEquals(
      RowWriteResult.Failure(ExpenseSheetFailure.Unexpected),
      gateway.appendCategories("sheet-1", listOf(Category("Pets", "", ""))),
    )
    assertEquals(1, server.requestCount)
  }

  private fun titles(vararg tabs: String) =
    json("""{"sheets":[${tabs.joinToString(",") { """{"properties":{"title":"$it"}}""" }}]}""")

  private fun headerRow(tab: String, cells: String) = json("""{"range":"$tab!1:1","values":[[$cells]]}""")

  @Test
  fun `repairLayout adds a deleted tab with its headers and a removed header into the first empty cell`() = runTest {
    server.enqueue(titles("Expenses", "categories", "Budgets", "Dismissed"))
    server.enqueue(json("{}"))
    server.enqueue(headerRow("Expenses", """"ID","Date","Amount","Category","Description","","SMS ID","Notes""""))
    server.enqueue(headerRow("Categories", """"name","Color","Icon""""))
    server.enqueue(headerRow("Budgets", """"Month","Category","Amount""""))
    server.enqueue(headerRow("Dismissed", """"SMS ID","Dismissed On""""))
    server.enqueue(json("{}"))

    assertEquals(RowWriteResult.Written(null), gateway.repairLayout("sheet-1"))

    val get = server.takeRequest()
    assertEquals("GET", get.method)
    assertEquals("/v4/spreadsheets/sheet-1", get.url.encodedPath)
    assertEquals("sheets.properties.title", get.url.queryParameter("fields"))
    val addSheet = server.takeRequest()
    assertEquals("POST", addSheet.method)
    assertEquals("/v4/spreadsheets/sheet-1:batchUpdate", addSheet.url.encodedPath)
    assertEquals("""{"requests":[{"addSheet":{"properties":{"title":"Settings"}}}]}""", addSheet.body!!.utf8())
    listOf("Expenses", "Categories", "Budgets", "Dismissed").forEach {
      assertEquals("/v4/spreadsheets/sheet-1/values/$it!1:1", server.takeRequest().url.encodedPath)
    }
    val headers = server.takeRequest()
    assertEquals("/v4/spreadsheets/sheet-1/values:batchUpdate", headers.url.encodedPath)
    assertEquals(
      """{"valueInputOption":"RAW","data":[{"range":"Expenses!F1","values":[["Source"]]},""" +
        """{"range":"Settings!A1","values":[["Key"]]},{"range":"Settings!B1","values":[["Value"]]}]}""",
      headers.body!!.utf8(),
    )
    assertEquals(7, server.requestCount)
  }

  @Test
  fun `repairLayout on a complete layout only reads`() = runTest {
    server.enqueue(titles("Expenses", "Categories", "Budgets", "Dismissed", "Settings"))
    server.enqueue(headerRow("Expenses", expensesHeader.removeSurrounding("[", "]")))
    server.enqueue(headerRow("Categories", """"Name","Color","Icon""""))
    server.enqueue(headerRow("Budgets", """"Month","Category","Amount""""))
    server.enqueue(headerRow("Dismissed", """"SMS ID","Dismissed On""""))
    server.enqueue(headerRow("Settings", """"Key","Value""""))

    assertEquals(RowWriteResult.Written(null), gateway.repairLayout("sheet-1"))

    assertEquals(6, server.requestCount)
    repeat(6) { assertEquals("GET", server.takeRequest().method) }
  }

  @Test
  fun `missing headers fill empty cells left to right, then past the end`() {
    fun writes(header: List<Any?>) =
      SheetLayoutRepair.headerWrites("Budgets", header, listOf("Month", "Category", "Amount")).map {
        it.range to it.values.single().single()
      }

    assertEquals(listOf("Budgets!B1" to "Category", "Budgets!C1" to "Amount"), writes(listOf("Month", null, " ", "Notes")))
    assertEquals(listOf("Budgets!C1" to "Category", "Budgets!D1" to "Amount"), writes(listOf("MONTH", "Notes")))
    assertEquals(emptyList<Pair<String, Any?>>(), writes(listOf(" month ", "category", "Amount")))
  }

  @Test
  fun `a failed repair maps a deleted sheet and stops before writing`() = runTest {
    server.enqueue(json("""{"error":{"code":404}}""", code = 404))
    assertEquals(RowWriteResult.SheetMissing, gateway.repairLayout("gone"))

    server.enqueue(titles("Expenses"))
    server.enqueue(json("{}", code = 500))
    assertEquals(RowWriteResult.Failure(ExpenseSheetFailure.Unexpected), gateway.repairLayout("sheet-1"))
    assertEquals(3, server.requestCount)
  }
}
