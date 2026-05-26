package com.lechenmusic.data.model

import com.google.gson.annotations.SerializedName

/** Wrapper for all Subsonic API responses */
data class SubsonicResponse(
    @SerializedName("subsonic-response") val subsonicResponse: SubsonicBody
)

data class SubsonicBody(
    val status: String = "",
    val version: String = "",
    val error: ErrorInfo? = null,
    val albumList: AlbumList? = null,
    val albumList2: AlbumList? = null,
    val searchResult2: SearchResult? = null,
    val searchResult3: SearchResult? = null,
    val artists: ArtistsWrapper? = null,
    val artist: ArtistDetail? = null,
    val album: AlbumDetail? = null,
    val playlists: PlaylistsWrapper? = null,
    val playlist: PlaylistDetail? = null,
    val songs: SongsWrapper? = null,
    val randomSongs: SongsWrapper? = null,
    val starred2: Starred2Data? = null,
    val starred: Starred2Data? = null,
    val star: Any? = null,
    val unstar: Any? = null,
    val starResult: StarResult? = null,
    val lyrics: LyricsData? = null,
    val songCount: Int? = null,
    val albumCount: Int? = null,
    val artistCount: Int? = null
)

data class ErrorInfo(
    val code: Int = 0,
    val message: String = ""
)

data class AlbumList(
    val album: List<Album>? = null
)

data class Album(
    val id: String = "",
    val name: String = "",
    val artist: String = "",
    val artistId: String = "",
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val playCount: Long = 0,
    val created: String = "",
    val year: Int? = null,
    val genre: String? = null
)

data class SearchResult(
    val song: List<Song>? = null,
    val album: List<Album>? = null,
    val artist: List<Artist>? = null
)

data class ArtistsWrapper(
    val index: List<ArtistIndex>? = null
)

data class ArtistIndex(
    val name: String = "",
    val artist: List<Artist>? = null
)

data class Artist(
    val id: String = "",
    val name: String = "",
    val coverArt: String? = null,
    val albumCount: Int = 0,
    val artistImageUrl: String? = null
)

data class ArtistDetail(
    val id: String = "",
    val name: String = "",
    val coverArt: String? = null,
    val albumCount: Int = 0,
    val artistImageUrl: String? = null,
    val album: List<Album>? = null
)

data class AlbumDetail(
    val id: String = "",
    val name: String = "",
    val artist: String = "",
    val artistId: String = "",
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val created: String = "",
    val year: Int? = null,
    val genre: String? = null,
    val song: List<Song>? = null
)

data class PlaylistsWrapper(
    val playlist: List<Playlist>? = null
)

data class Playlist(
    val id: String = "",
    val name: String = "",
    val comment: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val owner: String = "",
    val public: Boolean = false,
    val coverArt: String? = null
)

data class PlaylistDetail(
    val id: String = "",
    val name: String = "",
    val comment: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val owner: String = "",
    val public: Boolean = false,
    val coverArt: String? = null,
    val song: List<Song>? = null
)

data class SongsWrapper(
    val song: List<Song>? = null
)

data class Song(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val artistId: String = "",
    val album: String = "",
    val albumId: String = "",
    val coverArt: String? = null,
    val duration: Int = 0,
    val track: Int = 0,
    val year: Int? = null,
    val genre: String? = null,
    val size: Long = 0,
    val contentType: String = "",
    val suffix: String = "",
    val bitRate: Int = 0,
    val starred: String? = null,
    val playCount: Long = 0,
    val discNumber: Int = 0
) {
    val isStarred: Boolean get() = starred != null
    val durationFormatted: String get() {
        val min = duration / 60
        val sec = duration % 60
        return "%d:%02d".format(min, sec)
    }
}

data class StarResult(val status: String = "")

/** Data for getStarred2 response */
data class Starred2Data(
    val song: List<Song>? = null,
    val album: List<Album>? = null,
    val artist: List<Artist>? = null
)

data class LyricsData(
    val artist: String? = null,
    val title: String? = null,
    val value: String? = null
)
