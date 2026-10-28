package com.shashi.expensetracker.data.google

import com.shashi.expensetracker.data.account.DriveAuthorization
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/** The authenticated OkHttp client for Google APIs, kept apart from any other client. */
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class GoogleApi

/**
 * Provides the authenticated OkHttp client, Moshi and the Drive and Sheets Retrofit services. No
 * logging interceptor is installed, so no header, body or token reaches the log.
 */
@Module
@InstallIn(SingletonComponent::class)
object GoogleApiModule {
  const val DRIVE_BASE_URL = "https://www.googleapis.com/drive/v3/"
  const val SHEETS_BASE_URL = "https://sheets.googleapis.com/v4/"

  @Provides fun provideAccessTokenSource(impl: DriveAuthorization): AccessTokenSource = impl

  @Provides @Singleton fun provideMoshi(): Moshi = Moshi.Builder().build()

  @Provides
  @Singleton
  @GoogleApi
  fun provideOkHttpClient(tokens: AccessTokenSource): OkHttpClient = googleOkHttpClient(tokens)

  @Provides
  @Singleton
  fun provideDriveService(@GoogleApi client: OkHttpClient, moshi: Moshi): DriveService =
    retrofit(DRIVE_BASE_URL, client, moshi).create(DriveService::class.java)

  @Provides
  @Singleton
  fun provideSheetsService(@GoogleApi client: OkHttpClient, moshi: Moshi): SheetsService =
    retrofit(SHEETS_BASE_URL, client, moshi).create(SheetsService::class.java)

  /** The client every Google call goes through: bearer token in, one retry on 401. */
  fun googleOkHttpClient(tokens: AccessTokenSource): OkHttpClient =
    OkHttpClient.Builder()
      .addInterceptor(AuthInterceptor(tokens))
      .authenticator(TokenAuthenticator(tokens))
      .build()

  fun retrofit(baseUrl: String, client: OkHttpClient, moshi: Moshi): Retrofit =
    Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
}
