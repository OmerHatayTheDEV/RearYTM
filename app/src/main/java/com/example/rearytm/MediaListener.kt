package com.example.rearytm

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.service.notification.NotificationListenerService

class MediaListener : NotificationListenerService()

object Media {
    const val YTM = "com.google.android.apps.youtube.music"
    private fun prefs(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    fun selected(c: Context): Set<String> = prefs(c).getStringSet("pkgs", setOf(YTM))!!
    fun save(c: Context, s: Set<String>) = prefs(c).edit().putStringSet("pkgs", s).apply()

    fun sessions(c: Context): List<MediaController> = try {
        val sel = selected(c)
        c.getSystemService(MediaSessionManager::class.java)
            .getActiveSessions(ComponentName(c, MediaListener::class.java))
            .filter { it.packageName in sel }
    } catch (e: SecurityException) { emptyList() }
}
