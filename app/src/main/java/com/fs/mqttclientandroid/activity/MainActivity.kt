package com.fs.mqttclientandroid.activity

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import com.fs.mqttclientandroid.R
import com.fs.mqttclientandroid.mqtt.client.MessageDispatcher
import com.fs.mqttclientandroid.mqtt.client.MqttClient
import com.fs.mqttclientandroid.mqtt.config.ConnectConfig
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.client.mqttv3.MqttMessage
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity() {

    private var client: MqttClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initWebView()
    }

    override fun onStart() {
        super.onStart()

        client = MqttClient(this)
        client?.setDispatcher(Dispatcher())
    }

    override fun onResume() {
        super.onResume()

        val config = loadClientConfig()
        client?.connect(config)
    }

    override fun onPause() {
        super.onPause()

        client?.disconnect()
        client = null
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface", "AddJavascriptInterface")
    private fun initWebView() {
        webView.loadUrl("file:///android_asset/static/html/main.html")

        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true

        webView.addJavascriptInterface(JSInterface(), "main")
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
    }

    private fun loadClientConfig(): ConnectConfig {
        val inputStream = resources.assets.open("client_config/client_config.json")
        val result = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length: Int

        length = inputStream.read(buffer)
        while (length != -1) {
            result.write(buffer, 0, length)
            length = inputStream.read(buffer)
        }

        return Gson().fromJson(result.toString("UTF-8"),
                ConnectConfig::class.java)
    }

    private inner class Dispatcher: MessageDispatcher {

        @RequiresApi(Build.VERSION_CODES.KITKAT)
        override fun dispatch(client: MqttClient, topic: String, message: MqttMessage) {
            val result = String(message.payload)
            webView.evaluateJavascript("javascript:refresh_result(\"$result\")", null)
        }
    }

    private inner class JSInterface {

        @JavascriptInterface
        fun subscribe(topic: String, qos: Int) {
            client?.subscribe(topic, qos)
        }

        @JavascriptInterface
        fun publish(topic: String, message: String, qos: Int, retained: Boolean) {
            client?.publish(topic, message, qos, retained)
        }
    }
}
