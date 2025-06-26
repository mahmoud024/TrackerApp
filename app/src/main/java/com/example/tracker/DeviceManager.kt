package com.example.tracker

import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object DeviceManager {

    private const val PREFS_NAME = "tracker_prefs"
    private const val DEVICE_ID_KEY = "device_id"
    private const val SERVER_URL = "http://192.168.1.21:8083/api/devices"
    private const val USERNAME = "admin"
    private const val PASSWORD = "admin"

    fun getDeviceId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(DEVICE_ID_KEY, -1)
    }

    private fun saveDeviceId(context: Context, id: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(DEVICE_ID_KEY, id).apply()
    }

    fun registerDevice(context: Context, onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val name = "MATIA"
                val uniqueId = "Mahmoud159753"

                val credentials = "$USERNAME:$PASSWORD"
                val auth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

                // الخطوة 1: تحقق من وجود الجهاز مسبقًا
                val checkUrl = URL("$SERVER_URL?uniqueId=$uniqueId")
                val checkConnection = checkUrl.openConnection() as HttpURLConnection
                checkConnection.requestMethod = "GET"
                checkConnection.setRequestProperty("Authorization", auth)
                checkConnection.setRequestProperty("Accept", "application/json")

                val checkCode = checkConnection.responseCode
                if (checkCode == HttpURLConnection.HTTP_OK) {
                    val response = checkConnection.inputStream.bufferedReader().use { it.readText() }
                    val devicesArray = JSONArray(response)

                    if (devicesArray.length() > 0) {
                        val existingDevice = devicesArray.getJSONObject(0)
                        val id = existingDevice.getInt("id")
                        saveDeviceId(context, id)
                        onResult(true, "Device already registered with ID: $id")
                        return@Thread
                    }
                }

                // الخطوة 2: إذا لم يكن موجودًا، سجله
                val json = """
                {
                    "name": "$name",
                    "uniqueId": "$uniqueId"
                }
            """.trimIndent()

                val url = URL(SERVER_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Authorization", auth)

                connection.doOutput = true
                connection.outputStream.write(json.toByteArray())

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val id = jsonObject.getInt("id")
                    saveDeviceId(context, id)
                    onResult(true, "Device registered with ID: $id")
                } else {
                    val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    onResult(false, "Registration failed: $error")
                }

            } catch (e: Exception) {
                onResult(false, "Exception: ${e.message}")
                Log.e("DeviceManager", "Registration failed", e)
            }
        }.start()
    }

    fun checkDeviceExists(context: Context, id: Int, onResult: (Boolean) -> Unit) {
        Thread {
            try {
                val url = URL("$SERVER_URL/$id")
                val connection = url.openConnection() as HttpURLConnection
                val credentials = "$USERNAME:$PASSWORD"
                val auth = "Basic " + Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)

                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", auth)

                val responseCode = connection.responseCode
                onResult(responseCode == HttpURLConnection.HTTP_OK)

            } catch (e: Exception) {
                Log.e("DeviceManager", "Error checking device: ${e.message}")
                onResult(false)
            }
        }.start()
    }

    fun clearDeviceId(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(DEVICE_ID_KEY).apply()
    }



}
