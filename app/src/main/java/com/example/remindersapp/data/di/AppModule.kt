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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- 提供一个应用级的协程作用域 ---
    @Provides
    @Singleton
    fun provideApplicationScope(): CoroutineScope {
        // SupervisorJob 确保一个子协程的失败不会影响其他子协程
        // Dispatchers.Default 适用于 CPU 密集型任务，这里用于数据库操作很合适
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

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
    // UserSettingsRepository 带有 @Inject constructor 和 @Singleton，Hilt 会自动处理它的提供，
    // 我们无需在此处为其编写 @Provides 方法。

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

    // --- 核心修正：更新 RingtonePlayer 的提供者方法 ---
    @Provides
    @Singleton
    fun provideRingtonePlayer(
        @ApplicationContext context: Context,
        userSettingsRepository: UserSettingsRepository // <-- Hilt 会自动注入这个依赖
    ): RingtonePlayer {
        // 将获取到的依赖传递给 RingtonePlayer 的构造函数
        return RingtonePlayer(context, userSettingsRepository)
    }
}