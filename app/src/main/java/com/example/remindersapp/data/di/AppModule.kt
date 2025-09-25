package com.example.remindersapp.data.di

import android.content.Context
import androidx.room.Room
import com.example.remindersapp.data.*
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

/**
 * Hilt 模块，负责提供具体的实例 (使用 @Provides).
 * 比如数据库实例、DAO 实例，以及全局状态单例.
 */
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

/**
 * Hilt 模块，负责将接口与其实现进行绑定 (使用 @Binds).
 * @Binds 比 @Provides 更高效，是绑定接口的首选.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
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