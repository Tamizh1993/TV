package com.tamizh.tvlauncher.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

data class DayForecast(val day: String, val code: Int, val maxC: Double, val minC: Double)

data class Weather(
    val temperatureC: Double,
    val code: Int,
    val isDay: Boolean,
    /** Next few days (today excluded). */
    val forecast: List<DayForecast> = emptyList(),
)

data class Place(val name: String, val latitude: Double, val longitude: Double)

/**
 * Weather from Open-Meteo (https://open-meteo.com) — free, no API key needed.
 */
object WeatherApi {

    private fun httpGet(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("Accept", "application/json")
        try {
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    suspend fun findPlace(query: String): Place? = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search?count=1&language=en&format=json&name=" +
            URLEncoder.encode(query.trim(), "UTF-8")
        val results = JSONObject(httpGet(url)).optJSONArray("results")
        if (results == null || results.length() == 0) {
            null
        } else {
            val o = results.getJSONObject(0)
            Place(o.getString("name"), o.getDouble("latitude"), o.getDouble("longitude"))
        }
    }

    suspend fun current(lat: Double, lon: Double): Weather = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=5&timezone=auto"
        val root = JSONObject(httpGet(url))
        val c = root.getJSONObject("current")
        Weather(
            temperatureC = c.getDouble("temperature_2m"),
            code = c.getInt("weather_code"),
            isDay = c.optInt("is_day", 1) == 1,
            forecast = parseDaily(root.optJSONObject("daily")),
        )
    }
}

private fun parseDaily(daily: JSONObject?): List<DayForecast> {
    if (daily == null) return emptyList()
    val dates = daily.optJSONArray("time") ?: return emptyList()
    val codes = daily.optJSONArray("weather_code") ?: return emptyList()
    val maxes = daily.optJSONArray("temperature_2m_max") ?: return emptyList()
    val mins = daily.optJSONArray("temperature_2m_min") ?: return emptyList()
    val parse = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayName = SimpleDateFormat("EEE", Locale.getDefault())
    val out = ArrayList<DayForecast>()
    for (i in 1 until dates.length()) { // index 0 is today
        val label = runCatching { dayName.format(parse.parse(dates.getString(i))!!) }.getOrDefault("")
        val max = maxes.optDouble(i)
        if (max.isNaN()) continue
        out += DayForecast(label, codes.optInt(i), max, mins.optDouble(i))
    }
    return out
}

/** WMO weather code -> (emoji, short description). */
fun describeWeather(code: Int, isDay: Boolean): Pair<String, String> = when (code) {
    0 -> (if (isDay) "☀️" else "🌙") to "Clear"
    1, 2 -> (if (isDay) "🌤️" else "☁️") to "Partly cloudy"
    3 -> "☁️" to "Overcast"
    45, 48 -> "🌫️" to "Fog"
    51, 53, 55, 56, 57 -> "🌦️" to "Drizzle"
    61, 63, 65, 66, 67, 80, 81, 82 -> "🌧️" to "Rain"
    71, 73, 75, 77, 85, 86 -> "❄️" to "Snow"
    95, 96, 99 -> "⛈️" to "Thunderstorm"
    else -> "🌡️" to ""
}
