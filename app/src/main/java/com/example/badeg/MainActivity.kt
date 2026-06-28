package com.example.badeg

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    init { System.loadLibrary("native-lib") }
    external fun startServer(docRoot: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        // Siapkan folder web untuk Mongoose
        val webRoot = File(filesDir, "www")
        if (!webRoot.exists()) {
            webRoot.mkdirs()
            copyAssetsToStorage()
        }

        // Mulai server backend
        startServer(webRoot.absolutePath)
        webView.loadUrl("http://localhost:8080/index.html")

        // Logika Navigasi Bawah
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> webView.loadUrl("http://localhost:8080/index.html")
                R.id.nav_logs -> webView.loadUrl("http://localhost:8080/logs.html")
            }
            true
        }
    }

    private fun copyAssetsToStorage() {
        val assetManager = assets
        val files = assetManager.list("www") ?: return
        val outDir = File(filesDir, "www")
        for (filename in files) {
            val inStream = assetManager.open("www/$filename")
            val outFile = File(outDir, filename)
            val outStream = FileOutputStream(outFile)
            inStream.copyTo(outStream)
            inStream.close()
            outStream.close()
        }
    }
}
