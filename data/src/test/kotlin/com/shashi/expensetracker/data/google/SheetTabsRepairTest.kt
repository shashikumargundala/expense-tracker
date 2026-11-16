package com.shashi.expensetracker.data.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.domain.account.AccessResult
import com.shashi.expensetracker.domain.account.ConsentRequest
import com.shashi.expensetracker.domain.account.ExpenseRowFix
import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.RowWriteResult
import com.shashi.expensetracker.domain.inbox.Dismissal
import com.squareup.moshi.Moshi
import java.time.LocalDateTime
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The exact request bodies of AD-5's repair writes and the Dismissed append, through the real client. */
@RunWith(AndroidJUnit4::class)
class SheetTabsRepairTest {

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
  fun `repairs write only the ID, Source and SMS ID cells, in one USER_ENTERED batch`() = runTest {
    server.enqueue(json("""{"range":"Expenses!1:1","values":[["Notes","ID","Date","Amount","Category","Description","Source","SMS ID"]]}"""))
    server.enqueue(json("{}"))
    val fixes =
      listOf(
        ExpenseRowFix(4, id = "new-1", sourceUser = true),
        ExpenseRowFix(6, sourceUser = true),
        ExpenseRowFix(9, id = "new-2", clearSmsId = true),
      )

    assertEquals(RowWriteResult.Written(null), gateway.writeExpenseRepairs("sheet-1", fixes))

    assertEquals("/v4/spreadsheets/sheet-1/values/Expenses!1:1", server.takeRequest().url.encodedPath)
    val update = server.takeRequest()
    assertEquals("/v4/spreadsheets/sheet-1/values:batchUpdate", update.url.encodedPath)
    assertEquals(
      """{"valueInputOption":"USER_ENTERED","data":[""" +
        """{"range":"Expenses!B4","values":[["'new-1"]]},{"range":"Expenses!G4","values":[["User"]]},""" +
        """{"range":"Expenses!G6","values":[["User"]]},""" +
        """{"range":"Expenses!B9","values":[["'new-2"]]},{"range":"Expenses!H9","values":[[""]]}]}""",
      update.body!!.utf8(),
    )
    assertEquals(2, server.requestCount)
  }

  @Test
  fun `repairs send nothing without fixes and fail without a usable header`() = runTest {
    assertEquals(RowWriteResult.Written(null), gateway.writeExpenseRepairs("sheet-1", emptyList()))
    assertEquals(0, server.requestCount)

    server.enqueue(json("""{"range":"Expenses!1:1","values":[["Date","Amount"]]}"""))
    assertEquals(
      RowWriteResult.Failure(ExpenseSheetFailure.Unexpected),
      gateway.writeExpenseRepairs("sheet-1", listOf(ExpenseRowFix(2, id = "a"))),
    )
    assertEquals(1, server.requestCount)
  }

  @Test
  fun `deletes find the tab ID and remove rows bottom-up in one batchUpdate`() = runTest {
    server.enqueue(
      json("""{"sheets":[{"properties":{"sheetId":7,"title":"Categories"}},{"properties":{"sheetId":0,"title":"expenses"}}]}""")
    )
    server.enqueue(json("{}"))

    assertEquals(RowWriteResult.Written(null), gateway.deleteExpenseRows("sheet-1", listOf(4, 12, 7)))

    val get = server.takeRequest()
    assertEquals("GET", get.method)
    assertEquals("/v4/spreadsheets/sheet-1", get.url.encodedPath)
    assertEquals("sheets.properties(sheetId,title)", get.url.queryParameter("fields"))
    val update = server.takeRequest()
    assertEquals("/v4/spreadsheets/sheet-1:batchUpdate", update.url.encodedPath)
    fun delete(start: Int) =
      """{"deleteDimension":{"range":{"sheetId":0,"dimension":"ROWS","startIndex":$start,"endIndex":${start + 1}}}}"""
    assertEquals("""{"requests":[${delete(11)},${delete(6)},${delete(3)}]}""", update.body!!.utf8())
  }

  @Test
  fun `deletes send nothing for no rows and fail when the tab or the Sheet is gone`() = runTest {
    assertEquals(RowWriteResult.Written(null), gateway.deleteExpenseRows("sheet-1", emptyList()))
    assertEquals(0, server.requestCount)

    server.enqueue(json("""{"sheets":[{"properties":{"sheetId":7,"title":"Categories"}}]}"""))
    assertEquals(RowWriteResult.Failure(ExpenseSheetFailure.Unexpected), gateway.deleteExpenseRows("sheet-1", listOf(2)))
    assertEquals(1, server.requestCount)

    server.enqueue(json("""{"error":{"code":404}}""", code = 404))
    assertEquals(RowWriteResult.SheetMissing, gateway.deleteExpenseRows("gone", listOf(2)))

    server.enqueue(json("""{"sheets":[{"properties":{"sheetId":3,"title":"Expenses"}}]}"""))
    server.enqueue(json("{}", code = 503))
    assertEquals(RowWriteResult.Failure(ExpenseSheetFailure.Unexpected), gateway.deleteExpenseRows("sheet-1", listOf(2)))
  }

  @Test
  fun `a delete needing consent sends nothing`() = runTest {
    val request = object : ConsentRequest {}
    tokens.denied = AccessResult.NeedsUserConsent(request)

    assertEquals(RowWriteResult.NeedsConsent(request), gateway.deleteExpenseRows("sheet-1", listOf(2)))
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `Dismissals are appended as SMS ID and Dismissed On under the current header`() = runTest {
    server.enqueue(json("""{"range":"Dismissed!1:1","values":[["Dismissed On","Notes","SMS ID"]]}"""))
    server.enqueue(json("""{"updates":{"updatedRange":"Dismissed!A5:C6"}}"""))
    val on = LocalDateTime.of(2026, 11, 13, 9, 30)

    val result = gateway.appendDismissals("sheet-1", listOf(Dismissal("h:abc", on), Dismissal("ref:hdfc:1:1000", on)))

    assertEquals(RowWriteResult.Written(5), result)
    assertEquals("/v4/spreadsheets/sheet-1/values/Dismissed!1:1", server.takeRequest().url.encodedPath)
    val append = server.takeRequest()
    assertEquals("/v4/spreadsheets/sheet-1/values/Dismissed!A1:append", append.url.encodedPath)
    assertEquals("USER_ENTERED", append.url.queryParameter("valueInputOption"))
    assertEquals("INSERT_ROWS", append.url.queryParameter("insertDataOption"))
    val body = append.body!!.utf8()
    assertTrue(body, body.contains("""[["2026-11-13 09:30","","'h:abc"],["2026-11-13 09:30","","'ref:hdfc:1:1000"]]"""))
  }

  @Test
  fun `the Dismissed append sends nothing for none and fails without its headers`() = runTest {
    assertEquals(RowWriteResult.Written(null), gateway.appendDismissals("sheet-1", emptyList()))
    assertEquals(0, server.requestCount)

    server.enqueue(json("""{"range":"Dismissed!1:1","values":[["SMS ID"]]}"""))
    val result = gateway.appendDismissals("sheet-1", listOf(Dismissal("h:abc", LocalDateTime.of(2026, 11, 13, 9, 30))))

    assertEquals(RowWriteResult.Failure(ExpenseSheetFailure.Unexpected), result)
    assertEquals(1, server.requestCount)
  }
}
