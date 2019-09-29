package kg.delletenebre.yamus.media.library

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import kg.delletenebre.yamus.App
import kg.delletenebre.yamus.api.YandexApi
import kg.delletenebre.yamus.api.response.Station
import kg.delletenebre.yamus.api.response.Track
import kg.delletenebre.yamus.media.R
import kg.delletenebre.yamus.media.datasource.YandexDataSourceFactory
import kg.delletenebre.yamus.media.extensions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object CurrentPlaylist {
    const val TYPE_NONE = "none"
    const val TYPE_TRACKS = "tracks"
    const val TYPE_STATION = "station"
    private const val YAMUS_USER_AGENT = "Yandex-Music-API"
    private const val YAMUS_HEADER_X_YANDEX_MUSIC_CLIENT = "WindowsPhone/3.20"


    var id: String = ""
    var batchId: String = ""
    var tracks: MutableList<Track> = mutableListOf()
    var type: String = TYPE_NONE
    var station: Station? = null
    var mediaSource = ConcatenatingMediaSource()

    private val dataSourceFactory = YandexDataSourceFactory(YAMUS_USER_AGENT)

    init {
        dataSourceFactory.defaultRequestProperties
                .set("X-Yandex-Music-Client", YAMUS_HEADER_X_YANDEX_MUSIC_CLIENT)
    }

    suspend fun updatePlaylist(id: String, tracks: List<Track>, station: Station? = null) {
        this.id = id
        this.station = station
        this.tracks.clear()
        this.tracks.addAll(tracks)
        mediaSource.clear()
        mediaSource.addMediaSources(CurrentPlaylist.tracks.map {
            MediaMetadataCompat.Builder()
                    .from(it)
                    .apply {
                        albumArt = loadAlbumArt(it.coverUri)
                    }
                    .build()
        }.toMediaSource())
    }

    suspend fun addTracksToPlaylist(tracks: List<Track>) {
        this.tracks.addAll(tracks)
        mediaSource.addMediaSources(tracks.map {
            MediaMetadataCompat.Builder()
                    .from(it)
                    .apply {
                        albumArt = loadAlbumArt(it.coverUri)
                    }
                    .build()
        }.toMediaSource())
    }

    fun removeTrack(index: Int) {
        tracks.removeAt(index)
        mediaSource.removeMediaSource(index)
    }

    private suspend fun loadAlbumArt(url: String): Bitmap {
        return withContext(Dispatchers.IO) {
            Glide.with(App.instance.applicationContext)
                    .applyDefaultRequestOptions(RequestOptions()
                            .fallback(R.drawable.default_album_art)
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE))
                    .asBitmap()
                    .load(YandexApi.getImage(url, BrowseTreeObject.NOTIFICATION_LARGE_ICON_SIZE))
                    .submit(BrowseTreeObject.NOTIFICATION_LARGE_ICON_SIZE, BrowseTreeObject.NOTIFICATION_LARGE_ICON_SIZE)
                    .get()
        }
    }

    fun MediaMetadataCompat.Builder.from(track: Track): MediaMetadataCompat.Builder {
        var artistName = ""
        if (track.artists.isNotEmpty()) {
            artistName = track.artists[0].name
        }

        var albumTitle = ""
        var albumGenre = ""
        if (track.albums.isNotEmpty()) {
            val trackAlbum = track.albums[0]
            albumTitle = trackAlbum.title
            albumGenre = trackAlbum.genre

        }

        id = track.id
        title = track.title
        artist = artistName
        album = albumTitle
        duration = track.durationMs
        genre = albumGenre
        mediaUri = track.id
        albumArtUri = YandexApi.getImage(track.coverUri, 400)
        flag = MediaItem.FLAG_PLAYABLE
        explicit = if (track.contentWarning == "explicit") {
            1
        } else {
            0
        }

        displayTitle = track.title
        displaySubtitle = artistName
        displayDescription = albumTitle
        displayIconUri = YandexApi.getImage(track.coverUri, 400)

        // Add downloadStatus to force the creation of an "extras" bundle in the resulting
        // MediaMetadataCompat object. This is needed to send accurate metadata to the
        // media session during updates.
        downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED

        // Allow it to be used in the typical builder style.
        return this
    }

    inline val MediaMetadataCompat.fullDescription: MediaDescriptionCompat
        get() =
            description.also {
                it.extras?.putAll(bundle)
            }

    private fun MediaMetadataCompat.toMediaSource() =
            ProgressiveMediaSource.Factory(dataSourceFactory)
                    .setTag(fullDescription)
                    .createMediaSource(mediaUri)

    private fun List<MediaMetadataCompat>.toMediaSource(): List<ProgressiveMediaSource> {
        return map {
            it.toMediaSource()
        }
    }
}