package com.star_zero.pagingretrofitsample.paging

import android.arch.lifecycle.MutableLiveData
import android.arch.paging.PageKeyedDataSource
import com.star_zero.pagingretrofitsample.api.GitHubAPI
import com.star_zero.pagingretrofitsample.data.NetworkState
import com.star_zero.pagingretrofitsample.data.Repo
import com.xwray.groupie.Item
import timber.log.Timber
import java.io.IOException

class PageKeyedRepoDataSource<T: Item<*>>(
    private val api: GitHubAPI,
    private val converter: (Repo) -> T
) : PageKeyedDataSource<Int, T>() {

    val networkState = MutableLiveData<NetworkState>()

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, T>) {
        // 今回は使ってない
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, T>) {
        callAPI(params.key, params.requestedLoadSize) { repos, next ->
            callback.onResult(repos.map(converter), next)
        }
    }

    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, T>
    ) {
        callAPI(1, params.requestedLoadSize) { repos, next ->
            callback.onResult(repos.map(converter), null, next)
        }
    }

    private fun callAPI(
        page: Int,
        perPage: Int,
        callback: (repos: List<Repo>, next: Int?) -> Unit
    ) {
        Timber.d("page: $page, perPage: $perPage")

        networkState.postValue(NetworkState.RUNNING)

        var state = NetworkState.FAILED

        try {
            // とりあえずGoogleのリポジトリ一覧を取得
            val response = api.repos("google", page, perPage).execute()

            response.body()?.let {
                var next: Int? = null
                response.headers().get("Link")?.let {
                    // Headerにnextがあるかで次ページ有無を判断
                    val regex = Regex("rel=\"next\"")
                    if (regex.containsMatchIn(it)) {
                        next = page + 1
                    }
                }

                callback(it, next)
                state = NetworkState.SUCCESS
            }
        } catch (e: IOException) {
            Timber.w(e)
        }

        // 結果を通知
        networkState.postValue(state)
    }

}
