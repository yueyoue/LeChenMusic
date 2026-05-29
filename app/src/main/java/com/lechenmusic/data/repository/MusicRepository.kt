package com.lechenmusic.data.repository

import com.lechenmusic.data.api.ApiClient
import com.lechenmusic.data.api.SubsonicApi
import com.lechenmusic.data.model.*

class MusicRepository {
    private var api: SubsonicApi? = null
    private var serverUrl: String = ""
    private var username: String = ""
    private var password: String = ""

    fun configure(baseUrl: String, user: String, pass: String) {
        serverUrl = baseUrl
        username = user
        password = pass
        api = ApiClient.getApi(baseUrl)
    }

    fun getCoverArtUrl(coverArtId: String?): String? {
        return ApiClient.getCoverArtUrl(serverUrl, username, password, coverArtId)
    }

    fun getStreamUrl(songId: String): String {
        return ApiClient.getStreamUrl(serverUrl, username, password, songId)
    }

    suspend fun ping(): Result<Unit> {
        return try {
            val response = api!!.ping(username, password)
            if (response.subsonicResponse.status == "ok") {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.subsonicResponse.error?.message ?: "连接失败"))
            }
        } catch (e: Exception) {
            val msg = when {
                e.message?.contains("非JSON响应") == true -> "服务器地址错误或服务器未运行"
                e.message?.contains("格式错误") == true -> "服务器响应格式错误，请检查地址"
                e.message?.contains("timeout") == true -> "连接超时，请检查服务器地址"
                e.message?.contains("Unable to resolve host") == true -> "无法解析服务器地址，请检查是否正确"
                e.message?.contains("Connection refused") == true -> "连接被拒绝，请检查服务器端口"
                e.message?.contains("SSL") == true -> "SSL证书错误，请检查HTTPS配置"
                else -> e.message ?: "连接失败"
            }
            Result.failure(Exception(msg))
        }
    }

    suspend fun getNewestAlbums(size: Int = 20): Result<List<Album>> {
        return try {
            val response = api!!.getAlbumList2(username, password, type = "newest", size = size)
            Result.success(response.subsonicResponse.albumList2?.album ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentAlbums(size: Int = 20): Result<List<Album>> {
        return try {
            val response = api!!.getAlbumList2(username, password, type = "recent", size = size)
            Result.success(response.subsonicResponse.albumList2?.album ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFrequentAlbums(size: Int = 20): Result<List<Album>> {
        return try {
            val response = api!!.getAlbumList2(username, password, type = "frequent", size = size)
            Result.success(response.subsonicResponse.albumList2?.album ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRandomAlbums(size: Int = 20): Result<List<Album>> {
        return try {
            val response = api!!.getAlbumList2(username, password, type = "random", size = size)
            Result.success(response.subsonicResponse.albumList2?.album ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAlbum(id: String): Result<AlbumDetail> {
        return try {
            val response = api!!.getAlbum(username, password, id)
            val album = response.subsonicResponse.album
            if (album != null) Result.success(album)
            else Result.failure(Exception("专辑不存在"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getArtists(): Result<List<Artist>> {
        return try {
            val response = api!!.getArtists(username, password)
            val artists = response.subsonicResponse.artists?.index?.flatMap { it.artist ?: emptyList() } ?: emptyList()
            Result.success(artists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getArtist(id: String): Result<ArtistDetail> {
        return try {
            val response = api!!.getArtist(username, password, id)
            val artist = response.subsonicResponse.artist
            if (artist != null) Result.success(artist)
            else Result.failure(Exception("歌手不存在"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun search(query: String): Result<SearchResult> {
        return try {
            val response = api!!.search(username, password, query)
            Result.success(response.subsonicResponse.searchResult3 ?: SearchResult())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylists(): Result<List<Playlist>> {
        return try {
            val response = api!!.getPlaylists(username, password)
            Result.success(response.subsonicResponse.playlists?.playlist ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPlaylist(id: String): Result<PlaylistDetail> {
        return try {
            val response = api!!.getPlaylist(username, password, id)
            val playlist = response.subsonicResponse.playlist
            if (playlist != null) Result.success(playlist)
            else Result.failure(Exception("歌单不存在"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRandomSongs(size: Int = 20): Result<List<Song>> {
        return try {
            val response = api!!.getRandomSongs(username, password, size)
            // Subsonic API may return songs in "songs" or "randomSongs" field
            val songs = response.subsonicResponse.songs?.song
                ?: response.subsonicResponse.randomSongs?.song
                ?: emptyList()
            Result.success(songs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStarred(): Result<StarredData> {
        return try {
            val response = api!!.getStarred(username, password)
            val body = response.subsonicResponse
            val starred = body.starred2 ?: body.starred
            Result.success(StarredData(
                songs = starred?.song ?: emptyList(),
                albums = starred?.album ?: emptyList(),
                artists = starred?.artist ?: emptyList()
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLyrics(artist: String, title: String): Result<String?> {
        return try {
            val response = api!!.getLyrics(username, password, artist, title)
            // Check both 'value' and 'lyrics' fields for lyrics text
            val lyrics = response.subsonicResponse.lyrics?.text
            Result.success(lyrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServerStats(): Result<ServerStats> {
        return try {
            // Get real counts from server
            val playlistsResult = api!!.getPlaylists(username, password)
            val playlistCount = playlistsResult.subsonicResponse.playlists?.playlist?.size ?: 0

            // Get artist count from indexes
            val indexesResult = api!!.getIndexes(username, password)
            val artistCount = indexesResult.subsonicResponse.indexes?.index
                ?.flatMap { it.artist ?: emptyList() }?.size ?: 0

            // Get album count by paginating through all albums
            var albumCount = 0
            var albumOffset = 0
            val albumPageSize = 500
            while (true) {
                val page = api!!.getAlbumList2(username, password, type = "newest", size = albumPageSize, offset = albumOffset)
                val albums = page.subsonicResponse.albumList2?.album ?: emptyList()
                albumCount += albums.size
                if (albums.size < albumPageSize) break
                albumOffset += albumPageSize
                if (albumOffset > 10000) break // safety limit
            }

            // Get song count by paginating through all albums and summing songCount
            var songCount = 0
            var songAlbumOffset = 0
            while (true) {
                val page = api!!.getAlbumList2(username, password, type = "newest", size = albumPageSize, offset = songAlbumOffset)
                val albums = page.subsonicResponse.albumList2?.album ?: emptyList()
                songCount += albums.sumOf { it.songCount }
                if (albums.size < albumPageSize) break
                songAlbumOffset += albumPageSize
                if (songAlbumOffset > 10000) break
            }

            Result.success(ServerStats(
                songCount = songCount,
                albumCount = albumCount,
                playlistCount = playlistCount,
                artistCount = artistCount
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addToPlaylist(playlistId: String, songId: String): Result<Unit> {
        return try {
            api!!.updatePlaylist(username, password, playlistId, songIdToAdd = songId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFromPlaylist(playlistId: String, songIndex: Int): Result<Unit> {
        return try {
            api!!.removeFromPlaylist(username, password, playlistId, songIndex)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPlaylist(name: String, songId: String? = null, isPublic: Boolean = false): Result<String> {
        return try {
            val response = api!!.createPlaylist(username, password, name, songId = songId?.ifBlank { null })
            val playlistId = response.subsonicResponse.playlist?.id ?: ""
            // Set public visibility if requested
            if (playlistId.isNotBlank() && isPublic) {
                try {
                    api!!.updatePlaylist(username, password, playlistId, public = true)
                } catch (_: Exception) { }
            }
            Result.success(playlistId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllSongs(): Result<List<Song>> {
        return try {
            val allSongs = mutableListOf<Song>()
            val seenIds = mutableSetOf<String>()

            // Strategy 1: Use search3 with broad queries to get songs fast
            // Search with common single letters to cover most songs
            val searchQueries = listOf("a", "e", "i", "o", "u", "the", "s", "t", "n", "r")
            for (query in searchQueries) {
                try {
                    val result = api!!.search(username, password, query, songCount = 500)
                    val songs = result.subsonicResponse.searchResult3?.song ?: emptyList()
                    for (song in songs) {
                        if (seenIds.add(song.id)) {
                            allSongs.add(song)
                        }
                    }
                } catch (_: Exception) { }
            }

            // Strategy 2: Also get songs from starred, recent, frequent albums
            val albumTypes = listOf("newest", "recent", "frequent", "random", "starred")
            for (type in albumTypes) {
                try {
                    val page = api!!.getAlbumList2(username, password, type = type, size = 100)
                    val albums = page.subsonicResponse.albumList2?.album ?: emptyList()
                    for (album in albums) {
                        try {
                            val detail = api!!.getAlbum(username, password, album.id)
                            detail.subsonicResponse.album?.song?.forEach { song ->
                                if (seenIds.add(song.id)) {
                                    allSongs.add(song)
                                }
                            }
                        } catch (_: Exception) { }
                    }
                } catch (_: Exception) { }
            }

            if (allSongs.isNotEmpty()) {
                Result.success(allSongs)
            } else {
                // Final fallback: getRandomSongs
                val response = api!!.getRandomSongs(username, password, 500)
                val songs = response.subsonicResponse.songs?.song
                    ?: response.subsonicResponse.randomSongs?.song
                    ?: emptyList()
                Result.success(songs)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getIndexes(): Result<List<Artist>> {
        return try {
            val response = api!!.getIndexes(username, password)
            val artists = response.subsonicResponse.indexes?.index?.flatMap { it.artist ?: emptyList() } ?: emptyList()
            Result.success(artists)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun star(id: String): Result<Unit> {
        return try {
            api!!.star(username, password, id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unstar(id: String): Result<Unit> {
        return try {
            api!!.unstar(username, password, id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scrobble(id: String): Result<Unit> {
        return try {
            api!!.scrobble(username, password, id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePlaylistPublic(playlistId: String, isPublic: Boolean): Result<Unit> {
        return try {
            api!!.updatePlaylist(username, password, playlistId, public = isPublic)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInternetRadioStations(): Result<List<InternetRadioStation>> {
        return try {
            val response = api!!.getInternetRadioStations(username, password)
            val stations = response.subsonicResponse.internetRadioStations?.internetRadioStation ?: emptyList()
            Result.success(stations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class StarredData(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList()
)

data class ServerStats(
    val songCount: Int = 0,
    val albumCount: Int = 0,
    val playlistCount: Int = 0,
    val artistCount: Int = 0
)
