package com.kosta.healthtracker

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.kosta.healthtracker.utils.ThemePreferences

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val themePreferences = ThemePreferences(this)
        AppCompatDelegate.setDefaultNightMode(themePreferences.getThemeMode())
    }
}