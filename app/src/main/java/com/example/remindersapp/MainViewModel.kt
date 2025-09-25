package com.example.remindersapp
import androidx.lifecycle.ViewModel
import com.example.remindersapp.data.AppState
import com.example.remindersapp.worker.RingtonePlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
@HiltViewModel
class MainViewModel @Inject constructor(
    val appState: AppState,
    val ringtonePlayer: RingtonePlayer
) : ViewModel()