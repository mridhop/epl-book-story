package ua.acclorite.book_story.presentation.screens.history.data

import androidx.compose.foundation.lazy.LazyListState
import ua.acclorite.book_story.domain.model.GroupedHistory

data class HistoryState(
    val history: List<GroupedHistory> = emptyList(),
    val listState: LazyListState = LazyListState(0, 0),

    val isRefreshing: Boolean = false,
    val isLoading: Boolean = true,

    val showDeleteWholeHistoryDialog: Boolean = false,

    val showSearch: Boolean = false,
    val searchQuery: String = "",
    val hasFocused: Boolean = false,

    val scrollIndex: Int = 0,
    val scrollOffset: Int = 0,

    )
