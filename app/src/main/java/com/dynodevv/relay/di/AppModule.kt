package com.dynodevv.relay.di

import android.content.Context
import androidx.room.Room
import com.dynodevv.relay.data.local.AppDatabase
import com.dynodevv.relay.data.remote.api.OpenAICompatibleApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Protocol
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Logging) {
                level = LogLevel.NONE
            }
            engine {
                config {
                    // HTTP/1.1 only for SSE — HTTP/2 multiplexing can cause proxy buffering
                    protocols(listOf(Protocol.HTTP_1_1))
                    retryOnConnectionFailure(true)
                    // SSE needs infinite read timeout — server may pause between tokens
                    readTimeout(0, java.util.concurrent.TimeUnit.SECONDS)
                }
            }
        }
    }

    @Provides
    @Singleton
    fun provideOpenAICompatibleApi(client: HttpClient): OpenAICompatibleApi {
        return OpenAICompatibleApi(client)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "relay_database"
        ).build()
    }

    @Provides
    fun provideConversationDao(db: AppDatabase) = db.conversationDao()

    @Provides
    fun provideMessageDao(db: AppDatabase) = db.messageDao()

    @Provides
    fun provideProviderDao(db: AppDatabase) = db.providerDao()

    @Provides
    fun provideAiModelDao(db: AppDatabase) = db.aiModelDao()

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): com.dynodevv.relay.data.repository.SettingsRepository {
        return com.dynodevv.relay.data.repository.SettingsRepository(context)
    }
}
