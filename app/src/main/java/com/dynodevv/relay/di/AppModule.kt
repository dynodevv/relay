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
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android) {
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
                connectTimeout = 30000
                socketTimeout = 30000
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
}
