package com.mcal.common.data

import android.content.Context
import androidx.preference.PreferenceManager

object ReactivePreferences {
    @Volatile
    private var appContext: Context? = null

    @JvmStatic
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getContext(): Context =
        appContext ?: throw IllegalStateException("ReactivePreferences not initialized. Call init(context) from the application.")

    suspend fun getSigningVersion(): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(getContext())
        return try {
            prefs.getString("signing_version", "2")?.toInt() ?: 2
        } catch (e: Exception) {
            try {
                prefs.getInt("signing_version", 2)
            } catch (e2: Exception) {
                2
            }
        }
    }
}
