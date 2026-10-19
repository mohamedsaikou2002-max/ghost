package com.casper.lab

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small local-only activity log; never captures GPS, cameras, microphones, or passive identifiers. */
class SessionStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("casper_sessions", Context.MODE_PRIVATE)
    private val maxEntries = 100
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun add(action: String, detail: String) {
        val existing = getArray()
        val item = JSONObject().apply {
            put("at", dateFormat.format(Date()))
            put("action", action.take(80))
            put("detail", detail.take(500))
        }
        val output = JSONArray().apply {
            put(item)
            for (i in 0 until minOf(existing.length(), maxEntries - 1)) put(existing.get(i))
        }
        preferences.edit().putString("log", output.toString()).apply()
    }

    fun entries(): List<String> {
        val arr = getArray()
        return (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let { "${it.optString("at")} • ${it.optString("action")}\n${it.optString("detail")}" }
        }
    }

    fun exportText(): String = buildString {
        appendLine("CASPER LAB · local session log")
        appendLine("Experimental research records — not calibrated measurements unless explicitly identified.")
        appendLine()
        entries().forEach { appendLine(it); appendLine() }
    }

    fun clear() { preferences.edit().remove("log").apply() }

    private fun getArray() = try { JSONArray(preferences.getString("log", "[]")) } catch (_: Exception) { JSONArray() }
}
