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
            Result.success(response.subsonicResponse.randomSongs?.song ?: emptyList())
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
            val lyrics = response.subsonicResponse.lyrics?.value
            Result.success(lyrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getServerStats(): Result<ServerStats> {
        return try {
            // Try to get counts using multiple API calls with individual error handling
            var songCount = 0
            var albumCount = 0
            var playlistCount = 0

            // Get playlists count
            try {
                val playlistsResult = api!!.getPlaylists(username, password)
                playlistCount = playlistsResult.subsonicResponse.playlists?.playlist?.size ?: 0
            } catch (_: Exception) {}

            // Get album count via getAlbumList2 with large size
            try {
                val albumsResult = api!!.getAlbumList2(username, password, type = "newest", size = 500)
                albumCount = albumsResult.subsonicResponse.albumList2?.album?.size ?: 0
            } catch (_: Exception) {}

            // Get song count: try using getArtists and sum albumCount,
            // or fallback to getRandomSongs count
            try {
                val artistsResult = api!!.getArtists(username, password)
                val artists = artistsResult.subsonicResponse.artists?.index
                    ?.flatMap { it.artist ?: emptyList() } ?: emptyList()
                if (artists.isNotEmpty()) {
                    // Sum albumCount from all artists as approximate album count
                    val artistAlbumCount = artists.sumOf { it.albumCount }
                    if (artistAlbumCount > albumCount) albumCount = artistAlbumCount
                }
            } catch (_: Exception) {}

            // For song count, try random songs as a lower bound
            try {
                val songsResult = api!!.getRandomSongs(username, password, size = 500)
                songCount = songsResult.subsonicResponse.randomSongs?.song?.size ?: 0
            } catch (_: Exception) {}

            // If we got albums, try to estimate songs from album song counts
            if (albumCount > 0 && songCount < albumCount) {
                try {
                    val albumsResult = api!!.getAlbumList2(username, password, type = "newest", size = 500)
                    val albums = albumsResult.subsonicResponse.albumList2?.album ?: emptyList()
                    var totalSongs = 0
                    for (album in albums.take(20)) {
                        try {
                            val detail = api!!.getAlbum(username, password, album.id)
                            totalSongs += detail.subsonicResponse.album?.songCount ?: 0
                        } catch (_: Exception) {}
                    }
                    // Extrapolate if we only sampled some albums
                    if (albums.size > 20 && totalSongs > 0) {
                        songCount = (totalSongs.toLong() * albums.size / 20).toInt()
                    } else if (totalSongs > songCount) {
                        songCount = totalSongs
                    }
                } catch (_: Exception) {}
            }

            Result.success(ServerStats(
                songCount = songCount,
                albumCount = albumCount,
                playlistCount = playlistCount
            ))
        } catch (e: Exception) {
            // Even if everything fails, return empty stats instead of error
            Result.success(ServerStats())
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
}

data class StarredData(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList()
)

data class ServerStats(
    val songCount: Int = 0,
    val albumCount: Int = 0,
    val playlistCount: Int = 0
)
