package com.example.remindersapp.data.di

import android.content.Context
import androidx.room.Room
import com.example.remindersapp.data.*
import com.example.remindersapp.worker.ReminderScheduler
import com.example.remindersapp.worker.RingtonePlayer
import com.example.remindersapp.worker.Scheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- 提供数据库和 DAO ---
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

    // --- 提供接口的实现 ---
    // 使用 @Provides 同样可以实现接口绑定，逻辑更清晰
    @Provides
    @Singleton
    fun provideReminderRepository(dao: ReminderDao): ReminderRepository {
        return OfflineReminderRepository(dao)
    }

    @Provides
    @Singleton
    fun provideScheduler(@ApplicationContext context: Context): Scheduler {
        return ReminderScheduler(context)
    }

    // --- 提供全局单例 ---
    @Provides
    @Singleton
    fun provideAppState(): AppState = AppState()

    @Provides
    @Singleton
    fun provideRingtonePlayer(@ApplicationContext context: Context): RingtonePlayer {
        return RingtonePlayer(context)
    }
}