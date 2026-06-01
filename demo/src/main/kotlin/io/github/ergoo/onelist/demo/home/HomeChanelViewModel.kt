package io.github.ergoo.onelist.demo.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.ergoo.onelist.demo.home.horizontal.HorizontalVideo
import io.github.ergoo.onelist.demo.home.vertical.VerticalVideo
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChannelUiState(
    val rows: List<HomeChannelItem> = emptyList(),
    val arriveFinalPage: Boolean = false,
)

sealed interface ChannelUiEvent {

    object StartRefreshing : ChannelUiEvent

    data class RefreshFinished(val success: Boolean, val error: Throwable? = null) :
        ChannelUiEvent

    data class LoadMoreFinished(
        val success: Boolean,
        val isFinal: Boolean,
        val error: Throwable? = null
    ) :
        ChannelUiEvent
}

class HomeChanelViewModel(
    private val repository: PageRepository = PageRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelUiState())
    val uiState: StateFlow<ChannelUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChannelUiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ChannelUiEvent> = _events.asSharedFlow()

    private var lastRefreshJob: Job? = null

    val items
        get() = uiState.value.rows

    fun refresh() {
        lastRefreshJob?.cancel()
        lastLoadMoreJob?.cancel()

        lastRefreshJob = viewModelScope.launch {
            _events.tryEmit(ChannelUiEvent.StartRefreshing)
            runCatching {
                repository.refresh()
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        rows = result.items,
                        arriveFinalPage = !result.hasMore
                    )
                }
                _events.tryEmit(ChannelUiEvent.RefreshFinished(success = true))
            }.onFailure { throwable ->
                _events.tryEmit(
                    ChannelUiEvent.RefreshFinished(
                        success = false, throwable
                    )
                )
            }
        }
    }

    private var lastLoadMoreJob: Job? = null

    fun like(id: Long, like: Boolean) {
        val newData = uiState.value.rows.map {
            when (it) {
                is HorizontalVideo if it.id == id -> {
                    it.copy(liked = like)
                }

                is VerticalVideo if it.id == id -> {
                    it.copy(liked = like)
                }

                else -> {
                    it
                }
            }
        }
        _uiState.update {
            it.copy(
                rows = newData
            )
        }
    }

    fun loadMore() {
        lastLoadMoreJob?.cancel()

        lastLoadMoreJob = viewModelScope.launch {
            runCatching {
                val currentPages = uiState.value.rows
                repository.loadMore(currentPages.size)
            }.onSuccess { pageResult ->
                val newData = uiState.value.rows + pageResult.items
                _uiState.update {
                    it.copy(
                        rows = newData,
                        arriveFinalPage = !pageResult.hasMore
                    )
                }
                _events.tryEmit(ChannelUiEvent.LoadMoreFinished(true, !pageResult.hasMore))
            }.onFailure {
                _events.tryEmit(
                    ChannelUiEvent.LoadMoreFinished(
                        success = false,
                        isFinal = false,
                        error = it
                    )
                )
            }
        }
    }
}
