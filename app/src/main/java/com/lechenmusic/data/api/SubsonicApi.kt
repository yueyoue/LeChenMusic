package com.lechenmusic.data.api

import com.lechenmusic.data.model.SubsonicResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SubsonicApi {

    // Authentication
    @GET("rest/ping")
    suspend fun ping(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Albums
    @GET("rest/getAlbumList2")
    suspend fun getAlbumList2(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("type") type: String, // newest, recent, frequent, random, starred, alphabeticalByName, alphabeticalByArtist
        @Query("size") size: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getAlbum")
    suspend fun getAlbum(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Artists
    @GET("rest/getArtists")
    suspend fun getArtists(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getArtist")
    suspend fun getArtist(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Search
    @GET("rest/search3")
    suspend fun search(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("query") query: String,
        @Query("songCount") songCount: Int = 50,
        @Query("albumCount") albumCount: Int = 20,
        @Query("artistCount") artistCount: Int = 20,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getSongsByGenre")
    suspend fun getSongsByGenre(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("genre") genre: String,
        @Query("count") count: Int = 500,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Playlists
    @GET("rest/getPlaylists")
    suspend fun getPlaylists(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getPlaylist")
    suspend fun getPlaylist(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Songs
    @GET("rest/getRandomSongs")
    suspend fun getRandomSongs(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("size") size: Int = 20,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/getStarred2")
    suspend fun getStarred(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/star")
    suspend fun star(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    @GET("rest/unstar")
    suspend fun unstar(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Scrobble (report play)
    @GET("rest/scrobble")
    suspend fun scrobble(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("submission") submission: Boolean = true,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Get lyrics
    @GET("rest/getLyrics")
    suspend fun getLyrics(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("artist") artist: String,
        @Query("title") title: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Update playlist (add/remove songs)
    @GET("rest/updatePlaylist")
    suspend fun updatePlaylist(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("playlistId") playlistId: String,
        @Query("songIdToAdd") songIdToAdd: String? = null,
        @Query("name") name: String? = null,
        @Query("comment") comment: String? = null,
        @Query("public") public: Boolean? = null,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Create playlist
    @GET("rest/createPlaylist")
    suspend fun createPlaylist(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("name") name: String,
        @Query("songId") songId: String? = null,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Delete playlist
    @GET("rest/deletePlaylist")
    suspend fun deletePlaylist(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("id") id: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Remove song from playlist by index (Subsonic API uses songIndexToRemove)
    @GET("rest/updatePlaylist")
    suspend fun removeFromPlaylist(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("playlistId") playlistId: String,
        @Query("songIndexToRemove") songIndexToRemove: Int,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse

    // Get indexes (all artists grouped by index)
    @GET("rest/getIndexes")
    suspend fun getIndexes(
        @Query("u") username: String,
        @Query("p") password: String,
        @Query("v") version: String = "1.16.1",
        @Query("c") client: String = "lechenmusic",
        @Query("f") format: String = "json"
    ): SubsonicResponse
}
