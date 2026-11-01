package com.shashi.expensetracker.data.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.domain.account.ConsentRequest
import com.shashi.expensetracker.domain.account.AccessFailure
import com.shashi.expensetracker.domain.account.AccessResult
import com.shashi.expensetracker.domain.account.ExpenseSheetFailure
import com.shashi.expensetracker.domain.account.ExpenseSheetResult
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The find and create contract against `mockwebserver3`, through the real client and services. */
@RunWith(AndroidJUnit4::class)
class SheetsExpenseSheetGatewayTest {

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

  @After
  fun tearDown() = server.close()

  private fun json(body: String, code: Int = 200) =
    MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

  @Test
  fun `find uses the marker query and picks the oldest file`() = runTest {
    server.enqueue(
      json(
        """{"files":[{"id":"newer","createdTime":"2026-10-30T00:00:00.000Z"},{"id":"older","createdTime":"2026-10-01T00:00:00.000Z"}]}"""
      )
    )

    assertEquals(ExpenseSheetResult.Ready("older"), gateway.find())

    val url = server.takeRequest().url
    assertEquals("appProperties has { key='expenseTracker' and value='v1' } and trashed=false", url.queryParameter("q"))
    assertEquals("createdTime", url.queryParameter("orderBy"))
  }

  @Test
  fun `find with no files is NotFound`() = runTest {
    server.enqueue(json("""{"files":[]}"""))

    assertEquals(ExpenseSheetResult.NotFound, gateway.find())
  }

  @Test
  fun `find network failure is a Network failure`() = runTest {
    server.close()

    assertEquals(ExpenseSheetResult.Failure(ExpenseSheetFailure.Network), gateway.find())
  }

  @Test
  fun `find needing consent is passed through`() = runTest {
    val request = object : ConsentRequest {}
    tokens.denied = AccessResult.NeedsUserConsent(request)

    assertEquals(ExpenseSheetResult.NeedsConsent(request), gateway.find())
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `find with access denied is an Access failure`() = runTest {
    tokens.denied = AccessResult.Failure(AccessFailure.NotSignedIn)

    assertEquals(ExpenseSheetResult.Failure(ExpenseSheetFailure.Access), gateway.find())
  }

  @Test
  fun `create writes tabs, then values, then the marker last`() = runTest {
    server.enqueue(json("""{"spreadsheetId":"new-sheet"}"""))
    server.enqueue(json("{}"))
    server.enqueue(json("""{"id":"new-sheet"}"""))

    assertEquals(ExpenseSheetResult.Ready("new-sheet"), gateway.create())

    val create = server.takeRequest()
    val values = server.takeRequest()
    val marker = server.takeRequest()
    assertEquals("/v4/spreadsheets", create.url.encodedPath)
    val createBody = create.body!!.utf8()
    assertTrue(createBody.contains("Expense Tracker"))
    listOf("Expenses", "Categories", "Budgets", "Dismissed", "Settings").forEach { assertTrue(it, createBody.contains("\"$it\"")) }
    assertFalse(createBody.contains("appProperties"))

    assertEquals("/v4/spreadsheets/new-sheet/values:batchUpdate", values.url.encodedPath)
    val valuesBody = values.body!!.utf8()
    assertTrue(valuesBody.contains("USER_ENTERED"))
    assertTrue(valuesBody.contains("ID\",\"Date\",\"Amount\",\"Category\",\"Description\",\"Source\",\"SMS ID"))
    listOf("Food", "Transport", "Shopping", "Bills", "Fun").forEach { assertTrue(it, valuesBody.contains("\"$it\"")) }

    assertEquals("PATCH", marker.method)
    assertEquals("/drive/v3/files/new-sheet", marker.url.encodedPath)
    assertTrue(marker.body!!.utf8().contains("\"expenseTracker\":\"v1\""))
  }

  @Test
  fun `create failing before the marker never sets it`() = runTest {
    server.enqueue(json("""{"spreadsheetId":"new-sheet"}"""))
    server.enqueue(json("{}", code = 500))

    assertEquals(ExpenseSheetResult.Failure(ExpenseSheetFailure.Unexpected), gateway.create())

    assertEquals(2, server.requestCount)
  }

  @Test
  fun `create with no spreadsheet id fails`() = runTest {
    server.enqueue(json("{}"))

    assertEquals(ExpenseSheetResult.Failure(ExpenseSheetFailure.Unexpected), gateway.create())
  }
}
