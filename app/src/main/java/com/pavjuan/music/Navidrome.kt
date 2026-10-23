package com.pavjuan.music

import android.content.Context
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

// ⚠️ Test values only. Don't push these to GitHub.
object Session {
    var server = ""
    var user = ""
    var token = ""
    var salt = ""
    val isLoggedIn: Boolean get() = server.isNotEmpty() && token.isNotEmpty()

    private const val FILE = "juans_hifi"

    fun load(context: Context) {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        server = p.getString("server", "") ?: ""
        user = p.getString("user", "") ?: ""
        token = p.getString("token", "") ?: ""
        salt = p.getString("salt", "") ?: ""
    }

    fun save(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("server", server).putString("user", user)
            .putString("token", token).putString("salt", salt)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().clear().apply()
        server = ""; user = ""; token = ""; salt = ""
    }
}

val SERVER: String get() = Session.server

fun cleanServer(input: String): String {
    var s = input.trim().trimEnd('/')
    if (!s.startsWith("http://") && !s.startsWith("https://")) s = "https://$s"
    return s
}

fun logIn(context: Context, server: String, user: String, password: String) {
    val address = cleanServer(server)
    val name = user.trim()
    val salt = UUID.randomUUID().toString().take(8)
    val token = md5(password + salt)
    subsonic(download(
        "$address/rest/ping.view?u=${URLEncoder.encode(name, "UTF-8")}&t=$token&s=$salt&v=1.16.1&c=JuanMusic&f=json"
    ))
    Session.server = address
    Session.user = name
    Session.token = token
    Session.salt = salt
    Session.save(context)
}
fun md5(text: String): String =
    MessageDigest.getInstance("MD5").digest(text.toByteArray())
        .joinToString("") { "%02x".format(it) }

fun authParams(): String =
    "u=${URLEncoder.encode(Session.user, "UTF-8")}&t=${Session.token}&s=${Session.salt}&v=1.16.1&c=JuanMusic&f=json"
fun download(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 10_000
    conn.readTimeout = 10_000
    return conn.inputStream.bufferedReader().use { it.readText() }
}

fun subsonic(json: String): JSONObject {
    val response = JSONObject(json).getJSONObject("subsonic-response")
    if (response.getString("status") != "ok") {
        throw Exception(response.getJSONObject("error").getString("message"))
    }
    return response
}

data class Song(
    val id: String, val title: String, val artist: String, val album: String,
    val created: String = "", val coverArt: String = "",
    val albumId: String = "", val starred: Boolean = false,
    val duration: Int = 0
)
fun songFrom(s: JSONObject) = Song(
    s.getString("id"), s.optString("title"), s.optString("artist"), s.optString("album"),
    s.optString("created"), s.optString("coverArt"),
    s.optString("albumId"), s.has("starred"),
    s.optInt("duration")
)

fun fetchAllSongs(): List<Song> {
    val response = subsonic(download(
        "$SERVER/rest/search3.view?query=&songCount=500&albumCount=0&artistCount=0&${authParams()}"
    ))
    val array = response.getJSONObject("searchResult3").optJSONArray("song") ?: return emptyList()
    return (0 until array.length()).map { songFrom(array.getJSONObject(it)) }.sortedBy { it.title.lowercase() }
}

fun fetchAlbumSongs(albumId: String): List<Song> {
    val response = subsonic(download("$SERVER/rest/getAlbum.view?id=$albumId&${authParams()}"))
    val array = response.getJSONObject("album").optJSONArray("song") ?: return emptyList()
    return (0 until array.length()).map { songFrom(array.getJSONObject(it)) }
}

data class Album(val id: String, val name: String, val artist: String, val year: Int, val coverArt: String)

fun fetchAlbums(type: String, size: Int, extra: String = ""): List<Album> {
    val response = subsonic(download("$SERVER/rest/getAlbumList2.view?type=$type&size=$size$extra&${authParams()}"))
    val array = response.getJSONObject("albumList2").optJSONArray("album") ?: return emptyList()
    return (0 until array.length()).map { i ->
        val a = array.getJSONObject(i)
        Album(a.getString("id"), a.optString("name"), a.optString("artist"), a.optInt("year"), a.optString("coverArt"))
    }
}

fun streamUrl(id: String) = "$SERVER/rest/stream.view?id=$id&${authParams()}"

fun coverUrl(coverArt: String, size: Int = 300) =
    "$SERVER/rest/getCoverArt.view?id=$coverArt&size=$size&${authParams()}"

fun toMediaItem(song: Song): MediaItem =
    MediaItem.Builder()
        .setMediaId(song.id)
        .setUri(streamUrl(song.id))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setExtras(Bundle().apply {
                    putString("cover", song.coverArt)
                    putString("albumId", song.albumId)
                    putBoolean("starred", song.starred)
                })
                .build()
        )
        .build()

fun daysAgoStamp(days: Int): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(System.currentTimeMillis() - days * 86_400_000L))
}
fun setStar(songId: String? = null, albumId: String? = null, on: Boolean) {
    val command = if (on) "star" else "unstar"
    val target = if (songId != null) "id=$songId" else "albumId=$albumId"
    subsonic(download("$SERVER/rest/$command.view?$target&${authParams()}"))
}

fun isAlbumStarred(albumId: String): Boolean {
    val response = subsonic(download("$SERVER/rest/getAlbum.view?id=$albumId&${authParams()}"))
    return response.getJSONObject("album").has("starred")
}
data class Genre(val name: String, val songCount: Int, val albumCount: Int)

fun fetchGenres(): List<Genre> {
    val response = subsonic(download("$SERVER/rest/getGenres.view?${authParams()}"))
    val array = response.getJSONObject("genres").optJSONArray("genre") ?: return emptyList()
    return (0 until array.length()).map { i ->
        val g = array.getJSONObject(i)
        Genre(g.optString("value"), g.optInt("songCount"), g.optInt("albumCount"))
    }.filter { it.songCount > 0 && it.name.isNotBlank() }.sortedByDescending { it.songCount }
}

fun fetchGenreSongs(genre: String): List<Song> {
    val name = URLEncoder.encode(genre, "UTF-8")
    val response = subsonic(download("$SERVER/rest/getSongsByGenre.view?genre=$name&count=500&${authParams()}"))
    val array = response.getJSONObject("songsByGenre").optJSONArray("song") ?: return emptyList()
    return (0 until array.length()).map { songFrom(array.getJSONObject(it)) }
}

fun fetchGenreAlbums(genre: String): List<Album> =
    fetchAlbums("byGenre", 50, "&genre=" + URLEncoder.encode(genre, "UTF-8"))