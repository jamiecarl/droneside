package com.jamiecarl.droneside.data.storage

import android.content.Context
import android.content.SharedPreferences

/** Persists the user's chosen "home club" id — equivalent to the original ApplicationSettings usage. */
class HomeClubStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("droneside_prefs", Context.MODE_PRIVATE)

    fun setHomeClub(clubId: String) {
        prefs.edit().putString(KEY, clubId).apply()
    }

    fun getHomeClub(): String? = prefs.getString(KEY, null)

    fun removeHomeClub() {
        prefs.edit().remove(KEY).apply()
    }

    fun isHomeClub(clubId: String): Boolean = getHomeClub() == clubId

    private companion object {
        const val KEY = "homeClubId"
    }
}
