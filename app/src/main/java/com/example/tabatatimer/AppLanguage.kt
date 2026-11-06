package com.example.tabatatimer

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

internal fun currentAppLanguage(): String {
    val locales = AppCompatDelegate.getApplicationLocales()

    if (locales.isEmpty) {
        return ""
    }

    return when (locales[0]?.language) {
        "en" -> "en"
        "ru" -> "ru"
        else -> ""
    }
}

internal fun changeAppLanguage(
    languageTag: String
) {
    val locales = if (languageTag.isBlank()) {
        LocaleListCompat.getEmptyLocaleList()
    } else {
        LocaleListCompat.forLanguageTags(languageTag)
    }

    AppCompatDelegate.setApplicationLocales(locales)
}