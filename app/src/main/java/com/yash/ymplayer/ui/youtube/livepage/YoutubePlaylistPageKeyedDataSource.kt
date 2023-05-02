@file:JvmName("YoutubePlaylistPageKeyedDataSource")
package com.yash.ymplayer.ui.youtube.livepage

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.yash.ymplayer.repository.OnlineYoutubeRepository
import com.yash.ymplayer.util.YoutubeSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public class YoutubePlaylistPageKeyedDataSource(var context: Context, private val uniqueKey: String) :
    PagingSource<String, YoutubeSong>() {

    companion object {
        private const val TAG = "YoutubePlaylistPageKeyedDS"
    }

    override fun getRefreshKey(state: PagingState<String, YoutubeSong>): String? {
        TODO("Not yet implemented")
    }

    override suspend fun load(params: LoadParams<String>): LoadResult<String, YoutubeSong> {
        return if (params.key == null) {
            val tracks = withContext(Dispatchers.IO) {
                OnlineYoutubeRepository.getInstance(context).getPlaylistTracks(uniqueKey, "")
            }
            LoadResult.Page(tracks.items, tracks.prevToken, tracks.nextToken)
        } else {
            val tracks = withContext(Dispatchers.IO) {
                OnlineYoutubeRepository.getInstance(context).getMorePlaylistTracks(uniqueKey, params.key, "")
            }
            LoadResult.Page(tracks.items, null, tracks.nextToken)
        }
    }


}