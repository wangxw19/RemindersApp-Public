package com.example.remindersapp.data

import androidx.compose.ui.graphics.Color
import com.example.remindersapp.ui.theme.HighPriorityColor
import com.example.remindersapp.ui.theme.LowPriorityColor
import com.example.remindersapp.ui.theme.MediumPriorityColor
import com.example.remindersapp.ui.theme.NoPriorityColor

enum class Priority(val color: Color, val displayName: String) {
    HIGH(HighPriorityColor, "高"),
    MEDIUM(MediumPriorityColor, "中"),
    LOW(LowPriorityColor, "低"),
    NONE(NoPriorityColor, "无")
}