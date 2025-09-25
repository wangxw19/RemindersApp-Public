package com.example.remindersapp.data.di

import android.content.Context
import androidx.room.Room
import com.example.remindersapp.data.AppDatabase
import com.example.remindersapp.data.ReminderDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "reminders_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideReminderDao(appDatabase: AppDatabase): ReminderDao {
        return appDatabase.reminderDao()
    }
}