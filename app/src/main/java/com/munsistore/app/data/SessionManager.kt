package com.munsistore.app.data

import android.content.Context
import android.content.SharedPreferences

/** Tiny session store (SharedPreferences). No tokens are stored here in demo mode. */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("munsi_session", Context.MODE_PRIVATE)

    fun getUserEmail(): String? = prefs.getString(KEY_EMAIL, null)
    fun isAdmin(): Boolean = prefs.getBoolean(KEY_ADMIN, false)
    fun isLoggedIn(): Boolean = getUserEmail() != null
    fun getLanguage(): String = prefs.getString(KEY_LANG, "bn") ?: "bn"

    fun saveLogin(email: String, isAdmin: Boolean) {
        prefs.edit().putString(KEY_EMAIL, email).putBoolean(KEY_ADMIN, isAdmin).apply()
    }

    fun logout() {
        prefs.edit().remove(KEY_EMAIL).putBoolean(KEY_ADMIN, false).apply()
    }

    fun saveLanguage(lang: String) {
        prefs.edit().putString(KEY_LANG, lang).apply()
    }

    companion object {
        private const val KEY_EMAIL = "user_email"
        private const val KEY_ADMIN = "is_admin"
        private const val KEY_LANG = "language"
    }
}
