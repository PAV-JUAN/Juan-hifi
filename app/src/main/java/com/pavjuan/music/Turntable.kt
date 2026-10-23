package com.pavjuan.music

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Turntable {
    private var player: ExoPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var library: List<Song> = emptyList()

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

    fun init(context: Context) {
        if (player != null) return
        val p = ExoPlayer.Builder(context.applicationContext).build()
        p.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
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
            }
        })
        player = p
    }

    fun release() {
        player?.release()
        player = null
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
        if (p.playWhenReady) p.pause() else p.play()
    }

    fun pause() { player?.pause() }
    fun forget() {
        player?.stop()
        player?.clearMediaItems()
        library = emptyList()
        hasTrack = false
    }
    fun next() { player?.seekToNext() }
    fun previous() { player?.seekToPrevious() }
    fun seekTo(ms: Long) { player?.seekTo(ms) }    fun setRepeat(on: Boolean) {
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