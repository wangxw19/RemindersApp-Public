package com.example.remindersapp.data.di

import android.content.Context
import androidx.room.Room
import com.example.remindersapp.data.AppDatabase
import com.example.remindersapp.data.AppState
import com.example.remindersapp.data.ReminderDao
import com.example.remindersapp.data.OfflineReminderRepository
import com.example.remindersapp.data.ReminderRepository
import com.example.remindersapp.worker.ReminderScheduler
import com.example.remindersapp.worker.RingtonePlayer
import com.example.remindersapp.worker.Scheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "reminders_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideReminderDao(appDatabase: AppDatabase): ReminderDao {
        return appDatabase.reminderDao()
    }

    @Provides
    @Singleton
    fun provideAppState(): AppState = AppState()

    @Provides
    @Singleton
    fun provideRingtonePlayer(@ApplicationContext context: Context): RingtonePlayer {
        return RingtonePlayer(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindReminderRepository(
        offlineReminderRepository: OfflineReminderRepository
    ): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindScheduler(
        reminderScheduler: ReminderScheduler
    ): Scheduler
}