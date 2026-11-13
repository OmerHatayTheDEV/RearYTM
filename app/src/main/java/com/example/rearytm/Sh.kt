package com.example.rearytm

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

object Sh {
    const val CODE = 100
    const val COMP = "com.example.rearytm/com.example.rearytm.MediaListener"
    fun available() = try { Shizuku.pingBinder() } catch (e: Throwable) { false }
    fun granted() = available() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun exec(cmd: String): String {
        val m = Shizuku::class.java.getDeclaredMethod(
            "newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
        m.isAccessible = true
        val p = m.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
        val out = p.inputStream.bufferedReader().readText() + p.errorStream.bufferedReader().readText()
        p.waitFor()
        return out
    }

    fun setup(): String = exec("""
        cur=$(settings get secure enabled_notification_listeners)
        case "\$cur" in *$COMP*) ;; *)
          if [ -z "\$cur" ] || [ "\$cur" = null ]; then n=$COMP; else n="\$cur:$COMP"; fi
          settings put secure enabled_notification_listeners "\$n";;
        esac
        dumpsys deviceidle whitelist +com.example.rearytm
        cmd appops set com.example.rearytm RUN_IN_BACKGROUND allow
        echo OK
    """.trimIndent())
}
