package com.orbitterminal.app.di

import android.content.Context
import androidx.room.Room
import com.orbitterminal.app.data.db.OrbitDatabase
import com.orbitterminal.app.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideOrbitDatabase(@ApplicationContext context: Context): OrbitDatabase {
        return Room.databaseBuilder(
            context,
            OrbitDatabase::class.java,
            OrbitDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideChatDao(db: OrbitDatabase): ChatDao = db.chatDao()

    @Provides
    fun provideMessageDao(db: OrbitDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideTerminalSessionDao(db: OrbitDatabase): TerminalSessionDao = db.terminalSessionDao()

    @Provides
    fun provideTerminalOutputDao(db: OrbitDatabase): TerminalOutputDao = db.terminalOutputDao()

    @Provides
    fun provideAgentTaskDao(db: OrbitDatabase): AgentTaskDao = db.agentTaskDao()
}
