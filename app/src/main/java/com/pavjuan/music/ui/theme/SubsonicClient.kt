package com.pavjuan.music

import android.net.Uri
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.net.ssl.SSLException

/**
 * Talks to Navidrome using the Subsonic API.
 *
 * The password is never sent. Each request sends a "token" instead:
 *   token = md5(password + salt), where salt is a new random string every time.
 * Navidrome does the same math on its side and checks that the results match.
 */
object SubsonicClient {

    private const val API_VERSION = "1.16.1"
    private const val APP_NAME = "MyFirstApplication"

    /** The result of a ping: either it worked, or a message saying what went wrong. */
    sealed class PingResult {
        data class Ok(val serverType: String, val serverVersion: String) : PingResult()
        data class Error(val message: String) : PingResult()
    }

    /**
     * Asks the server "are you there, and are these login details right?"
     * This does network work, so it must NOT run on the screen's main thread.
     */
    fun ping(server: String, username: String, password: String): PingResult {
        return try {
            val body = get(server, "ping", username, password)
            val response = JSONObject(body).getJSONObject("subsonic-response")

            if (response.optString("status") == "ok") {
                PingResult.Ok(
                    serverType = response.optString("type", "Subsonic server"),
                    serverVersion = response.optString("serverVersion", response.optString("version"))
                )
            } else {
                val error = response.optJSONObject("error")
                when (error?.optInt("code")) {
                    40 -> PingResult.Error("Wrong username or password")
                    else -> PingResult.Error(error?.optString("message") ?: "The server said no")
                }
            }
        } catch (e: UnknownHostException) {
            PingResult.Error("Can't find that server address")
        } catch (e: SocketTimeoutException) {
            PingResult.Error("The server took too long to answer")
        } catch (e: SSLException) {
            PingResult.Error("Secure connection failed (HTTPS problem)")
        } catch (e: Exception) {
            PingResult.Error("Couldn't connect: ${e.message}")
        }
    }

    /** Builds a Subsonic API address with the login token, sends it, returns the reply as text. */
    private fun get(server: String, endpoint: String, username: String, password: String): String {
        val salt = randomSalt()
        val token = md5(password + salt)

        val url = Uri.parse(server.trimEnd('/'))
            .buildUpon()
            .appendEncodedPath("rest/$endpoint.view")
            .appendQueryParameter("u", username)
            .appendQueryParameter("t", token)
            .appendQueryParameter("s", salt)
            .appendQueryParameter("v", API_VERSION)
            .appendQueryParameter("c", APP_NAME)
            .appendQueryParameter("f", "json")
            .build()
            .toString()

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000   // 10 seconds to connect
        connection.readTimeout = 10_000      // 10 seconds to get an answer
        try {
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            return stream?.bufferedReader()?.use { it.readText() } ?: ""
        } finally {
            connection.disconnect()
        }
    }

    /** 12 random hex characters, new for every request. */
    private fun randomSalt(): String {
        val bytes = ByteArray(6)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** The MD5 fingerprint of a text, written as 32 hex characters. */
    private fun md5(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}