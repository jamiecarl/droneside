package com.jamiecarl.droneside.data.storage

import android.content.Context
import android.content.SharedPreferences

/** Which AUFPV club site the Live tab is watching (its base URL). */
class LiveClubStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("droneside_prefs", Context.MODE_PRIVATE)

    fun get(): String? = prefs.getString(KEY, null)

    fun set(baseUrl: String) {
        prefs.edit().putString(KEY, baseUrl).apply()
    }

    private companion object {
        const val KEY = "liveClubUrl"
    }
}
