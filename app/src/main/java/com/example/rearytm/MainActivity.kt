package com.example.rearytm

import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.provider.Settings
import android.view.Display
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 96, 48, 48) }
        fun btn(t: String, a: () -> Unit) = Button(this).apply { text = t; setOnClickListener { a() } }.also { root.addView(it) }
        root.addView(TextView(this).apply { text = "Arka ekran medya kumandası"; textSize = 20f })
        status = TextView(this).also { root.addView(it) }
        btn("1) Shizuku ile yetki ver (önerilen)") { shizukuSetup() }
        btn("   veya elle bildirim erişimi") { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        btn("2) Kontrol edilecek uygulamaları seç") { pickApps() }
        btn("3) Arka ekranda aç") { openOnRear() }
        btn("Ön ekranda önizle") { startActivity(Intent(this, RearActivity::class.java)) }
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        status.text = "Shizuku: " + when {
            Sh.granted() -> "yetkili ✓"
            Sh.available() -> "çalışıyor, izin gerekli"
            else -> "kapalı / başlatılmamış"
        } + "\nSeçili: ${Media.selected(this).size} uygulama"
    }

    private fun shizukuSetup() {
        if (!Sh.available()) { toast("Önce Shizuku'yu başlat"); return }
        if (Sh.granted()) { runSetup(); return }
        Shizuku.addRequestPermissionResultListener(object : Shizuku.OnRequestPermissionResultListener {
            override fun onRequestPermissionResult(code: Int, result: Int) {
                Shizuku.removeRequestPermissionResultListener(this)
                if (result == 0) runSetup() else toast("İzin reddedildi")
            }
        })
        Shizuku.requestPermission(Sh.CODE)
    }

    private fun runSetup() = Thread {
        val r = try { Sh.setup() } catch (e: Throwable) { "Hata: ${e.message}" }
        runOnUiThread { toast(if (r.contains("OK")) "Yetkiler verildi ✓" else r); onResume() }
    }.start()

    private fun pickApps() {
        val pm = packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
            .distinctBy { it.first }.sortedBy { it.second.lowercase() }
        val sel = Media.selected(this)
        val checked = BooleanArray(apps.size) { apps[it].first in sel }
        AlertDialog.Builder(this).setTitle("Uygulamaları seç")
            .setMultiChoiceItems(apps.map { it.second }.toTypedArray(), checked) { _, i, c -> checked[i] = c }
            .setPositiveButton("Kaydet") { _, _ ->
                Media.save(this, apps.indices.filter { checked[it] }.map { apps[it].first }.toSet()); onResume()
            }.setNegativeButton("İptal", null).show()
    }

    private fun openOnRear() {
        val rear = getSystemService(DisplayManager::class.java).displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY }
        if (rear == null) { toast("Arka ekran bulunamadı"); return }
        if (Sh.granted()) {
            Thread {
                val r = try { Sh.exec("am start --display ${rear.displayId} -n com.example.rearytm/.RearActivity") } catch (e: Throwable) { e.message }
                runOnUiThread { if (r?.contains("Error") == true) toast(r) }
            }.start()
            return
        }
        try {
            startActivity(Intent(this, RearActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                ActivityOptions.makeBasic().setLaunchDisplayId(rear.displayId).toBundle())
        } catch (e: Exception) { toast("Açılamadı: ${e.message}") }
    }

    private fun toast(s: String?) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
