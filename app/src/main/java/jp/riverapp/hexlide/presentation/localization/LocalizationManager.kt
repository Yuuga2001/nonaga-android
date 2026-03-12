package jp.riverapp.hexlide.presentation.localization

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalizationManager @Inject constructor(
    private val prefs: SharedPreferences
) {
    enum class Language(val nativeName: String) {
        SYSTEM(""),
        EN("English"),
        JA("日本語"),
        KO("한국어"),
        ZH_HANS("简体中文"),
        ZH_HANT("繁體中文"),
        ES("Español"),
        FR("Français"),
        DE("Deutsch"),
        PT("Português"),
        IT("Italiano"),
        RU("Русский"),
        TH("ไทย"),
        VI("Tiếng Việt"),
        ID("Bahasa Indonesia"),
        TR("Türkçe");
    }

    private val _language = MutableStateFlow(loadSavedLanguage())
    val language: StateFlow<Language> = _language.asStateFlow()

    fun setLanguage(lang: Language) {
        _language.value = lang
        prefs.edit().putString(PREF_KEY, lang.name).apply()
    }

    val resolvedLanguage: Language
        get() = if (_language.value == Language.SYSTEM) detectSystemLanguage() else _language.value

    val strings: LocalizedStrings
        get() = stringsFor(resolvedLanguage)

    private fun loadSavedLanguage(): Language {
        val saved = prefs.getString(PREF_KEY, null) ?: return Language.SYSTEM
        return try {
            Language.valueOf(saved)
        } catch (_: IllegalArgumentException) {
            Language.SYSTEM
        }
    }

    companion object {
        private const val PREF_KEY = "hexlide_lang"

        fun detectSystemLanguage(): Language {
            val locale = java.util.Locale.getDefault()
            val lang = locale.language
            val country = locale.country
            val tag = locale.toLanguageTag()

            return when {
                lang == "ja" -> Language.JA
                lang == "ko" -> Language.KO
                tag.startsWith("zh-Hans") || tag.startsWith("zh-CN") -> Language.ZH_HANS
                tag.startsWith("zh-Hant") || tag.startsWith("zh-TW") || tag.startsWith("zh-HK") -> Language.ZH_HANT
                lang == "zh" && (country == "CN" || country == "SG") -> Language.ZH_HANS
                lang == "zh" -> Language.ZH_HANT
                lang == "es" -> Language.ES
                lang == "fr" -> Language.FR
                lang == "de" -> Language.DE
                lang == "pt" -> Language.PT
                lang == "it" -> Language.IT
                lang == "ru" -> Language.RU
                lang == "th" -> Language.TH
                lang == "vi" -> Language.VI
                lang == "id" || lang == "ms" -> Language.ID
                lang == "tr" -> Language.TR
                else -> Language.EN
            }
        }

        fun stringsFor(language: Language): LocalizedStrings = when (language) {
            Language.SYSTEM, Language.EN -> LocalizedStrings.EN
            Language.JA -> LocalizedStrings.JA
            Language.KO -> LocalizedStrings.KO
            Language.ZH_HANS -> LocalizedStrings.ZH_HANS
            Language.ZH_HANT -> LocalizedStrings.ZH_HANT
            Language.ES -> LocalizedStrings.ES
            Language.FR -> LocalizedStrings.FR
            Language.DE -> LocalizedStrings.DE
            Language.PT -> LocalizedStrings.PT
            Language.IT -> LocalizedStrings.IT
            Language.RU -> LocalizedStrings.RU
            Language.TH -> LocalizedStrings.TH
            Language.VI -> LocalizedStrings.VI
            Language.ID -> LocalizedStrings.ID
            Language.TR -> LocalizedStrings.TR
        }
    }
}
