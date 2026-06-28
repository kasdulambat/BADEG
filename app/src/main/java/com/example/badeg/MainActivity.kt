package com.example.badeg

import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupWebView()
        prepareAssets()
        startMongooseService()
        setupNavigation()
    }

    private fun setupWebView() {
        webView = findViewById(R.id.webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
        }
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        webView.loadUrl("http://localhost:8080/index.html")
    }

    private fun prepareAssets() {
        val webRoot = File(filesDir, "www")
        if (!webRoot.exists()) {
            webRoot.mkdirs()
            copyAssetsToStorage()
        }
    }

    private fun startMongooseService() {
        val webRoot = File(filesDir, "www")
        val intent = Intent(this, MongooseService::class.java).apply {
            putExtra("DOC_ROOT", webRoot.absolutePath)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun setupNavigation() {
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
        try {
            val assetManager = assets
            val files = assetManager.list("www") ?: return
            val outDir = File(filesDir, "www")
            for (filename in files) {
                assetManager.open("www/$filename").use { inStream ->
                    File(outDir, filename).outputStream().use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
            }
        } catch (e: Exception) {
            ServerManager.nativeLog("Error copying assets: ${e.message}")
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun showToast(message: String) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }

        @JavascriptInterface
        fun log(message: String) {
            ServerManager.nativeLog("WEB: $message")
        }
    }
}
