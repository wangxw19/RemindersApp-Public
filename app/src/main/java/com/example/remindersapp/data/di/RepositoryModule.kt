package com.example.remindersapp.data.di

import com.example.remindersapp.data.OfflineReminderRepository
import com.example.remindersapp.data.ReminderRepository
import com.example.remindersapp.worker.ReminderScheduler
import com.example.remindersapp.worker.Scheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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