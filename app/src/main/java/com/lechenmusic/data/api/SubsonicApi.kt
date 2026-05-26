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

    // Get indexes / artists count via getArtists (already have this)
    // Get album count via getAlbumList2 with size=1 — but better to use getAlbumList
    // Subsonic doesn't have a direct "count" endpoint; we derive from list sizes.
    // We'll use getAlbumList2 with a large size to count.

    // Get recent play history (Subsonic doesn't have a direct "recently played songs" endpoint,
    // but getAlbumList2 type="recent" gives recently played albums.
    // For songs, we'll track locally via addRecentPlay.
}
