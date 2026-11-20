package com.pavjuan.music

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object Turntable {
    var player: ExoPlayer? = null
        private set
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var library: List<Song> = emptyList()
    private var prefs: SharedPreferences? = null

    // where the last song is remembered (same private storage as the login)
    private const val KEY_QUEUE = "last_queue"
    private const val KEY_INDEX = "last_index"
    private const val KEY_POS = "last_position"

    var title by mutableStateOf("")
    var artist by mutableStateOf("")
    var album by mutableStateOf("")
    var cover by mutableStateOf("")
    var songId by mutableStateOf("")
    var albumId by mutableStateOf("")
    var songStarred by mutableStateOf(false)
    var albumStarred by mutableStateOf(false)
    var isPlaying by mutableStateOf(false)
    var wantsPlay by mutableStateOf(false)
    var hasTrack by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var repeatAll by mutableStateOf(false)

    @OptIn(UnstableApi::class)
    fun init(context: Context) {
        if (player != null) return
        prefs = context.applicationContext.getSharedPreferences("juans_hifi", Context.MODE_PRIVATE)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,  // keep at least 15 s downloaded ahead
                60_000,  // never download more than 60 s ahead
                1_500,   // start playing once 1.5 s is ready (default 2.5 s)
                3_000    // after a stall, resume once 3 s is ready
            )
            .build()
        val p = ExoPlayer.Builder(context.applicationContext)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) saveSpot()          // paused or stopped: remember the exact spot
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                wantsPlay = playWhenReady
            }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                val meta = item?.mediaMetadata
                val extras = meta?.extras
                title = meta?.title?.toString() ?: ""
                artist = meta?.artist?.toString() ?: ""
                album = meta?.albumTitle?.toString() ?: ""
                cover = extras?.getString("cover") ?: ""
                songId = item?.mediaId ?: ""
                songStarred = extras?.getBoolean("starred", false) ?: false
                hasTrack = item != null
                val newAlbum = extras?.getString("albumId") ?: ""
                if (newAlbum != albumId) {
                    albumId = newAlbum
                    albumStarred = false
                    if (newAlbum.isNotEmpty()) checkAlbumStar(newAlbum)
                }
                saveSpot()                         // new song: remember it
            }
        })
        player = p
        restoreSpot(context)
    }

    fun release() {
        saveSpot()
        player?.release()
        player = null
    }

    /** Remember the current list of songs, which one is playing, and where in it. */
    fun saveSpot() {
        val p = player ?: return
        val store = prefs ?: return
        if (p.mediaItemCount == 0) {
            store.edit().remove(KEY_QUEUE).remove(KEY_INDEX).remove(KEY_POS).apply()
            return
        }
        val queue = JSONArray()
        for (i in 0 until p.mediaItemCount) {
            val item = p.getMediaItemAt(i)
            val meta = item.mediaMetadata
            val extras = meta.extras
            queue.put(JSONObject().apply {
                put("id", item.mediaId)
                put("title", meta.title?.toString() ?: "")
                put("artist", meta.artist?.toString() ?: "")
                put("album", meta.albumTitle?.toString() ?: "")
                put("cover", extras?.getString("cover") ?: "")
                put("albumId", extras?.getString("albumId") ?: "")
                put("starred", extras?.getBoolean("starred", false) ?: false)
            })
        }
        store.edit()
            .putString(KEY_QUEUE, queue.toString())
            .putInt(KEY_INDEX, p.currentMediaItemIndex)
            .putLong(KEY_POS, p.currentPosition)
            .apply()
    }

    /** On start-up, put the last remembered song back in the player, paused. */
    private fun restoreSpot(context: Context) {
        val p = player ?: return
        val store = prefs ?: return
        if (p.mediaItemCount > 0) return                       // something is already loaded
        if (!Session.isLoggedIn) Session.load(context.applicationContext)
        if (!Session.isLoggedIn) return                        // no login, no stream links
        val saved = store.getString(KEY_QUEUE, null) ?: return
        val songs = try {
            val arr = JSONArray(saved)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                Song(
                    id = o.getString("id"),
                    title = o.optString("title"),
                    artist = o.optString("artist"),
                    album = o.optString("album"),
                    coverArt = o.optString("cover"),
                    albumId = o.optString("albumId"),
                    starred = o.optBoolean("starred")
                )
            }
        } catch (e: Exception) {
            return                                             // unreadable save: just start empty
        }
        if (songs.isEmpty()) return
        val index = store.getInt(KEY_INDEX, 0).coerceIn(0, songs.lastIndex)
        val position = store.getLong(KEY_POS, 0L).coerceAtLeast(0L)
        p.setMediaItems(songs.map { toMediaItem(it) }, index, position)
        p.prepare()                                            // ready, but no play(): stays paused
    }

    fun playSongs(songs: List<Song>, start: Int = 0) {
        val p = player ?: return
        if (songs.isEmpty()) return
        p.setMediaItems(songs.map { toMediaItem(it) }, start, 0L)
        p.prepare()
        p.play()
    }

    fun toggle() {
        val p = player ?: return
        if (p.playWhenReady && p.playbackState != Player.STATE_IDLE) {
            p.pause()
            return
        }
        when (p.playbackState) {
            Player.STATE_IDLE -> p.prepare()
            Player.STATE_ENDED -> p.seekToDefaultPosition(p.currentMediaItemIndex)
        }
        p.play()
    }

    fun pause() { player?.pause() }
    fun forget() {
        player?.stop()
        player?.clearMediaItems()
        library = emptyList()
        hasTrack = false
        prefs?.edit()?.remove(KEY_QUEUE)?.remove(KEY_INDEX)?.remove(KEY_POS)?.apply()
    }
    fun next() { player?.seekToNext() }
    fun previous() { player?.seekToPrevious() }
    fun seekTo(ms: Long) { player?.seekTo(ms) }
    fun setRepeat(on: Boolean) {
        repeatAll = on
        player?.repeatMode = if (on) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }
    fun positionMs(): Long = player?.currentPosition ?: 0L
    fun durationMs(): Long = player?.duration?.takeIf { it > 0 } ?: 0L

    fun playAlbum(albumId: String) = safely {
        playSongs(withContext(Dispatchers.IO) { fetchAlbumSongs(albumId) })
    }

    fun playShuffle(count: Int = 25) = safely {
        if (library.isEmpty()) library = withContext(Dispatchers.IO) { fetchAllSongs() }
        playSongs(library.shuffled().take(count))
    }

    fun toggleSongStar() = safely {
        val id = songId
        val on = !songStarred
        songStarred = on
        try {
            withContext(Dispatchers.IO) { setStar(songId = id, on = on) }
        } catch (e: Exception) {
            songStarred = !on
            throw e
        }
    }

    fun toggleAlbumStar() = safely {
        val id = albumId
        if (id.isEmpty()) {
            error = "This song has no album id, so it can't be saved as a favorite"
            return@safely
        }
        val on = !albumStarred
        albumStarred = on
        try {
            withContext(Dispatchers.IO) { setStar(albumId = id, on = on) }
        } catch (e: Exception) {
            albumStarred = !on
            throw e
        }
    }

    private fun checkAlbumStar(id: String) = safely {
        val starred = withContext(Dispatchers.IO) { isAlbumStarred(id) }
        if (albumId == id) albumStarred = starred
    }

    private fun safely(block: suspend () -> Unit) {
        scope.launch {
            try {
                error = null
                block()
            } catch (e: Exception) {
                error = e.message
            }
        }
    }
}
