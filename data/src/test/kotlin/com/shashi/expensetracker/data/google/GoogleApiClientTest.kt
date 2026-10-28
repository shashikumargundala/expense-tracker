package com.shashi.expensetracker.data.google

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shashi.expensetracker.domain.account.AccessFailure
import com.shashi.expensetracker.domain.account.AccessResult
import com.shashi.expensetracker.domain.account.ConsentRequest
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
import org.robolectric.shadows.ShadowLog

/** Each I/O matrix row of story 2.2, through the real OkHttp client and Retrofit services. */
@RunWith(AndroidJUnit4::class)
class GoogleApiClientTest {

  private val server = MockWebServer()
  private val tokens = FakeTokenSource()
  private lateinit var drive: DriveService
  private lateinit var sheets: SheetsService

  private object TestConsent : ConsentRequest

  @Before
  fun setUp() {
    ShadowLog.clear()
    server.start()
    val client = GoogleApiModule.googleOkHttpClient(tokens)
    val moshi = Moshi.Builder().build()
    drive = GoogleApiModule.retrofit(server.url("/drive/v3/").toString(), client, moshi).create(DriveService::class.java)
    sheets = GoogleApiModule.retrofit(server.url("/v4/").toString(), client, moshi).create(SheetsService::class.java)
  }

  @After
  fun tearDown() {
    server.close()
    ShadowLog.getLogs().forEach { log ->
      assertFalse("Token logged: ${log.tag}", "token-" in log.msg || log.throwable?.message?.contains("token-") == true)
    }
  }

  private fun json(body: String, code: Int = 200) =
    MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

  private fun unauthorized() = json("""{"error":{"code":401,"status":"UNAUTHENTICATED"}}""", code = 401)

  private suspend fun listFiles() =
    googleApiCall { drive.listFiles(q = "trashed=false", orderBy = "createdTime", fields = "files(id)", pageSize = 10) }

  @Test
  fun grantedTokenIsSentAsBearer() = runTest {
    server.enqueue(json("""{"files":[{"id":"sheet-1"}]}"""))

    val result = listFiles()

    assertEquals(ApiResult.Success(FileList(listOf(DriveFile(id = "sheet-1")))), result)
    val request = server.takeRequest()
    assertEquals("Bearer token-1", request.headers["Authorization"])
    assertEquals("/drive/v3/files", request.url.encodedPath)
    assertEquals("trashed=false", request.url.queryParameter("q"))
    assertEquals("createdTime", request.url.queryParameter("orderBy"))
    assertEquals("files(id)", request.url.queryParameter("fields"))
    assertEquals("10", request.url.queryParameter("pageSize"))
  }

  @Test
  fun cachedTokenIsReusedWithoutANewToken() = runTest {
    server.enqueue(json("""{"files":[]}"""))
    server.enqueue(json("""{"files":[]}"""))

    listFiles()
    listFiles()

    assertEquals("Bearer token-1", server.takeRequest().headers["Authorization"])
    assertEquals("Bearer token-1", server.takeRequest().headers["Authorization"])
    assertEquals(1, tokens.issued)
  }

  @Test
  fun expiredTokenIsClearedRefetchedAndRetriedOnce() = runTest {
    server.enqueue(unauthorized())
    server.enqueue(json("""{"files":[{"id":"sheet-1"}]}"""))

    val result = listFiles()

    assertEquals(ApiResult.Success(FileList(listOf(DriveFile(id = "sheet-1")))), result)
    assertEquals(listOf("token-1"), tokens.invalidated)
    assertEquals("Bearer token-1", server.takeRequest().headers["Authorization"])
    assertEquals("Bearer token-2", server.takeRequest().headers["Authorization"])
    assertEquals(2, server.requestCount)
  }

  @Test
  fun secondUnauthorizedStopsAfterOneRetryAsAuthFailure() = runTest {
    server.enqueue(unauthorized())
    server.enqueue(unauthorized())
    server.enqueue(json("""{"files":[]}"""))

    val result = listFiles()

    assertEquals(ApiResult.Failure(ApiFailure.Access(AccessFailure.Unauthorized)), result)
    assertEquals(2, server.requestCount)
    assertEquals(listOf("token-1"), tokens.invalidated)
  }

  @Test
  fun consentNeededSendsNoRequest() = runTest {
    tokens.denied = AccessResult.NeedsUserConsent(TestConsent)

    assertEquals(ApiResult.Failure(ApiFailure.NeedsUserConsent(TestConsent)), listFiles())
    assertEquals(0, server.requestCount)
  }

  @Test
  fun signedOutSendsNoRequest() = runTest {
    tokens.denied = AccessResult.Failure(AccessFailure.NotSignedIn)

    assertEquals(ApiResult.Failure(ApiFailure.Access(AccessFailure.NotSignedIn)), listFiles())
    assertEquals(0, server.requestCount)
  }

  @Test
  fun consentNeededWhileRefetchingAfter401IsReturnedWithoutRetry() = runTest {
    server.enqueue(unauthorized())
    server.enqueue(json("""{"files":[]}"""))
    var first = true
    val source =
      object : AccessTokenSource by tokens {
        override suspend fun fetchToken(): TokenResult =
          if (first) tokens.fetchToken().also { first = false }
          else TokenResult.Denied(AccessResult.NeedsUserConsent(TestConsent))
      }
    val client = GoogleApiModule.googleOkHttpClient(source)
    val service =
      GoogleApiModule.retrofit(server.url("/drive/v3/").toString(), client, Moshi.Builder().build())
        .create(DriveService::class.java)

    val result = googleApiCall { service.listFiles("trashed=false", null, null, null) }

    assertEquals(ApiResult.Failure(ApiFailure.NeedsUserConsent(TestConsent)), result)
    assertEquals(1, server.requestCount)
  }

  @Test
  fun otherHttpErrorsKeepTheirCode() = runTest {
    server.enqueue(json("""{"error":{"code":403}}""", code = 403))

    assertEquals(ApiResult.Failure(ApiFailure.Http(403)), listFiles())
    assertEquals(1, server.requestCount)
  }

  @Test
  fun noServerIsNetworkFailure() = runTest {
    server.close()

    assertEquals(ApiResult.Failure(ApiFailure.Network), listFiles())
  }

  @Test
  fun updateFileSendsOnlyAppPropertiesAsPatch() = runTest {
    server.enqueue(json("""{"id":"sheet-1","appProperties":{"expenseTracker":"v1"}}"""))

    val result =
      googleApiCall { drive.updateFile("sheet-1", DriveFile(appProperties = mapOf("expenseTracker" to "v1")), "id,appProperties") }

    assertEquals(ApiResult.Success(DriveFile(id = "sheet-1", appProperties = mapOf("expenseTracker" to "v1"))), result)
    val request = server.takeRequest()
    assertEquals("PATCH", request.method)
    assertEquals("/drive/v3/files/sheet-1", request.url.encodedPath)
    assertEquals("""{"appProperties":{"expenseTracker":"v1"}}""", request.body?.utf8())
    assertEquals("Bearer token-1", request.headers["Authorization"])
  }

  @Test
  fun createSpreadsheetPostsTitleAndTabs() = runTest {
    server.enqueue(json("""{"spreadsheetId":"abc123"}"""))
    val body =
      Spreadsheet(
        properties = SpreadsheetProperties(title = "Expense Tracker"),
        sheets = listOf(Sheet(SheetProperties(title = "Expenses"))),
      )

    val result = googleApiCall { sheets.createSpreadsheet(body, "spreadsheetId") }

    assertEquals(ApiResult.Success(Spreadsheet(spreadsheetId = "abc123")), result)
    val request = server.takeRequest()
    assertEquals("POST", request.method)
    assertEquals("/v4/spreadsheets", request.url.encodedPath)
    assertEquals(
      """{"properties":{"title":"Expense Tracker"},"sheets":[{"properties":{"title":"Expenses"}}]}""",
      request.body?.utf8(),
    )
  }

  @Test
  fun batchUpdateValuesPostsToTheValuesEndpoint() = runTest {
    server.enqueue(json("""{"spreadsheetId":"abc123","totalUpdatedCells":2}"""))
    val body =
      BatchUpdateValuesRequest(
        valueInputOption = BatchUpdateValuesRequest.USER_ENTERED,
        data = listOf(ValueRange(range = "Settings!A1:B1", values = listOf(listOf("Key", "Value")))),
      )

    val result = googleApiCall { sheets.batchUpdateValues("abc123", body) }

    assertEquals(ApiResult.Success(Unit), result)
    val request = server.takeRequest()
    assertTrue(request.url.encodedPath.endsWith("/v4/spreadsheets/abc123/values:batchUpdate"))
    assertEquals(
      """{"valueInputOption":"USER_ENTERED","data":[{"range":"Settings!A1:B1","values":[["Key","Value"]]}]}""",
      request.body?.utf8(),
    )
  }
}
