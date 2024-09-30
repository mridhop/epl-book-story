@file:Suppress("LABEL_NAME_CLASH")

package ua.acclorite.book_story.presentation.screens.browse.data

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import ua.acclorite.book_story.domain.model.NullableBook
import ua.acclorite.book_story.domain.model.SelectableFile
import ua.acclorite.book_story.domain.use_case.GetBookFromFile
import ua.acclorite.book_story.domain.use_case.GetFilesFromDevice
import ua.acclorite.book_story.domain.use_case.InsertBook
import ua.acclorite.book_story.domain.use_case.UpdateFavoriteDirectory
import ua.acclorite.book_story.presentation.core.navigation.Screen
import ua.acclorite.book_story.presentation.core.util.BaseViewModel
import ua.acclorite.book_story.presentation.core.util.launchActivity
import ua.acclorite.book_story.presentation.data.MainState
import ua.acclorite.book_story.presentation.screens.settings.nested.browse.data.BrowseFilesStructure
import ua.acclorite.book_story.presentation.screens.settings.nested.browse.data.BrowseSortOrder
import javax.inject.Inject

@OptIn(ExperimentalPermissionsApi::class)
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val getBookFromFile: GetBookFromFile,
    private val getFilesFromDevice: GetFilesFromDevice,
    private val insertBook: InsertBook,
    private val updateFavoriteDirectory: UpdateFavoriteDirectory,
) : BaseViewModel<BrowseState, BrowseEvent>() {

    private val _state = MutableStateFlow(BrowseState())
    override val state = _state.asStateFlow()

    private var searchQueryJob: Job? = null
    private var refreshListJob: Job? = null
    private var getBooksJob: Job? = null
    private var storagePermissionJob: Job? = null
    private var changeDirectoryJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    isLoading = true,
                    selectedDirectory = Environment.getExternalStorageDirectory(),
                    inNestedDirectory = false
                )
            }
            getFilesFromDownloads()
        }
    }

    override fun onEvent(event: BrowseEvent) {
        when (event) {
            is BrowseEvent.OnStoragePermissionRequest -> handleStoragePermissionRequest(event)
            is BrowseEvent.OnStoragePermissionDismiss -> handleStoragePermissionDismiss(event)
            is BrowseEvent.OnRefreshList -> refreshList()
            is BrowseEvent.OnPermissionCheck -> checkPermission(event)
            is BrowseEvent.OnSelectFile -> selectFile(event)
            is BrowseEvent.OnSelectFiles -> selectFiles(event)
            is BrowseEvent.OnSelectBook -> selectBook(event)
            is BrowseEvent.OnSearchShowHide -> toggleSearch()
            is BrowseEvent.OnRequestFocus -> requestFocus(event)
            is BrowseEvent.OnClearSelectedFiles -> clearSelectedFiles()
            is BrowseEvent.OnSearchQueryChange -> updateSearchQuery(event)
            is BrowseEvent.OnSearch -> searchFiles()
            is BrowseEvent.OnAddingDialogDismiss -> dismissAddingDialog()
            is BrowseEvent.OnAddingDialogRequest -> requestAddingDialog()
            is BrowseEvent.OnGetBooksFromFiles -> getBooksFromFiles()
            is BrowseEvent.OnAddBooks -> addBooks(event)
            is BrowseEvent.OnLoadList -> loadList()
            is BrowseEvent.OnUpdateScrollOffset -> updateScrollOffset()
            is BrowseEvent.OnChangeDirectory -> changeDirectory(event)
            is BrowseEvent.OnGoBackDirectory -> goBackDirectory()
            is BrowseEvent.OnShowHideFilterBottomSheet -> toggleFilterBottomSheet()
            is BrowseEvent.OnScrollToFilterPage -> scrollToFilterPage(event)
            is BrowseEvent.OnUpdateFavoriteDirectory -> updateFavoriteDirectory(event)
        }
    }

    private fun launchSettingsIntent(activity: ComponentActivity, action: String) {
        val uri = Uri.parse("package:${activity.packageName}")
        val intent = Intent(action, uri)
        var failure = false
        intent.launchActivity(activity) {
            failure = true
        }
        if (failure) {
            return
        }
    }

    private fun handleStoragePermissionRequest(event: BrowseEvent.OnStoragePermissionRequest) {
        val legacyStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
        val isPermissionGranted = if (legacyStoragePermission) {
            event.storagePermissionState.status.isGranted
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                false
            }
        }

        if (isPermissionGranted) {
            _state.update {
                it.copy(
                    requestPermissionDialog = false,
                    isError = false
                )
            }
            onEvent(BrowseEvent.OnRefreshList)
            return
        }

        if (legacyStoragePermission) {
            if (!event.storagePermissionState.status.shouldShowRationale) {
                event.storagePermissionState.launchPermissionRequest()
            } else {
                launchSettingsIntent(event.activity, Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            }
        } else {
            launchSettingsIntent(event.activity, Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        }

        monitorStoragePermission(event, legacyStoragePermission)
    }

    private fun monitorStoragePermission(event: BrowseEvent.OnStoragePermissionRequest, legacyStoragePermission: Boolean) {
        storagePermissionJob?.cancel()
        storagePermissionJob = viewModelScope.launch {
            while (true) {
                val granted = if (legacyStoragePermission) {
                    event.storagePermissionState.status.isGranted
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Environment.isExternalStorageManager()
                    } else {
                        false
                    }
                }

                if (!granted) {
                    delay(1000)
                    yield()
                    continue
                }

                yield()

                _state.update {
                    it.copy(
                        requestPermissionDialog = false,
                        isError = false
                    )
                }
                onEvent(BrowseEvent.OnRefreshList)
                break
            }
        }
    }

    private fun handleStoragePermissionDismiss(event: BrowseEvent.OnStoragePermissionDismiss) {
        val legacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
        val isPermissionGranted = if (!legacyPermission) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                false
            }
        } else {
            event.permissionState.status.isGranted
        }

        storagePermissionJob?.cancel()
        _state.update {
            it.copy(
                requestPermissionDialog = false
            )
        }

        if (isPermissionGranted) {
            viewModelScope.launch(Dispatchers.IO) {
                getFilesFromDownloads()
            }
        } else {
            _state.update {
                it.copy(
                    isError = true
                )
            }
        }
    }

    private fun refreshList() {
        refreshListJob?.cancel()
        refreshListJob = viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    isRefreshing = true,
                    hasSelectedItems = false,
                    showSearch = false,
                    listState = LazyListState(),
                    gridState = LazyGridState(),
                )
            }
            yield()
            getFilesFromDownloads("")
            delay(500)
            _state.update {
                it.copy(
                    isRefreshing = false
                )
            }
        }
    }

    private fun checkPermission(event: BrowseEvent.OnPermissionCheck) {
        viewModelScope.launch(Dispatchers.IO) {
            val legacyPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
            val isPermissionGranted = if (!legacyPermission) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    false
                }
            } else {
                event.permissionState.status.isGranted
            }

            if (isPermissionGranted) {
                return@launch
            }

            _state.update {
                it.copy(
                    requestPermissionDialog = true,
                    isError = false
                )
            }
        }
    }

    private fun selectFile(event: BrowseEvent.OnSelectFile) {
        viewModelScope.launch(Dispatchers.IO) {
            val editedList = _state.value.selectableFiles.map { file ->
                when (event.file.isDirectory) {
                    false -> {
                        if (event.file.fileOrDirectory.path == file.fileOrDirectory.path) {
                            file.copy(
                                isSelected = !file.isSelected
                            )
                        } else {
                            file
                        }
                    }

                    true -> {
                        if (
                            file.fileOrDirectory.path.startsWith(
                                event.file.fileOrDirectory.path
                            ) && event.includedFileFormats.run {
                                if (isEmpty()) return@run true
                                any {
                                    file.fileOrDirectory.path.endsWith(
                                        it, ignoreCase = true
                                    ) || file.isDirectory
                                }
                            }
                        ) {
                            file.copy(
                                isSelected = !event.file.isSelected
                            )
                        } else {
                            file
                        }
                    }
                }
            }

            _state.update {
                it.copy(
                    selectableFiles = editedList,
                    selectedItemsCount = editedList.filter { file ->
                        file.isSelected && !file.isDirectory
                    }.size.run {
                        if (this == 0) return@run it.selectedItemsCount
                        this
                    },
                    hasSelectedItems = editedList.any { file ->
                        file.isSelected && !file.isDirectory
                    }
                )
            }
        }
    }

    private fun selectFiles(event: BrowseEvent.OnSelectFiles) {
        viewModelScope.launch(Dispatchers.IO) {
            val editedList = _state.value.selectableFiles.map { file ->
                if (
                    event.files.any {
                        file.fileOrDirectory.path.startsWith(it.fileOrDirectory.path)
                    } && event.includedFileFormats.run {
                        if (isEmpty()) return@run true
                        any {
                            file.fileOrDirectory.path.endsWith(
                                it, ignoreCase = true
                            ) || file.isDirectory
                        }
                    }
                ) {
                    file.copy(isSelected = true)
                } else {
                    file
                }
            }

            _state.update {
                it.copy(
                    selectableFiles = editedList,
                    selectedItemsCount = editedList.filter { file ->
                        file.isSelected && !file.isDirectory
                    }.size.run {
                        if (this == 0) return@run it.selectedItemsCount
                        this
                    },
                    hasSelectedItems = editedList.any { file ->
                        file.isSelected && !file.isDirectory
                    }
                )
            }
        }
    }

    private fun selectBook(event: BrowseEvent.OnSelectBook) {
        viewModelScope.launch(Dispatchers.IO) {
            val indexOfFile = _state.value.selectedBooks.indexOf(event.book)

            if (indexOfFile == -1) {
                return@launch
            }

            val editedList = _state.value.selectedBooks.toMutableList()
            editedList[indexOfFile] = editedList[indexOfFile].first to
                    !editedList[indexOfFile].second

            if (!editedList.filter { it.first is NullableBook.NotNull }
                    .any { it.second }
            ) {
                return@launch
            }

            _state.update {
                it.copy(
                    selectedBooks = editedList
                )
            }
        }
    }

    private fun toggleSearch() {
        viewModelScope.launch(Dispatchers.IO) {
            val shouldHide = _state.value.showSearch

            if (shouldHide) {
                getFilesFromDownloads("")
            } else {
                _state.update {
                    it.copy(
                        searchQuery = "",
                        hasSearched = false,
                        hasFocused = false
                    )
                }
            }
            _state.update {
                it.copy(
                    showSearch = !shouldHide
                )
            }
        }
    }

    private fun requestFocus(event: BrowseEvent.OnRequestFocus) {
        if (!_state.value.hasFocused) {
            event.focusRequester.requestFocus()
            _state.update {
                it.copy(
                    hasFocused = true
                )
            }
        }
    }

    private fun clearSelectedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    selectableFiles = it.selectableFiles.map { file ->
                        file.copy(
                            isSelected = false
                        )
                    },
                    hasSelectedItems = false
                )
            }
        }
    }

    private fun updateSearchQuery(event: BrowseEvent.OnSearchQueryChange) {
        _state.update {
            it.copy(
                searchQuery = event.query
            )
        }
        searchQueryJob?.cancel()
        searchQueryJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            yield()
            onEvent(BrowseEvent.OnSearch)
        }
    }

    private fun searchFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            getFilesFromDownloads()
        }
    }

    private fun dismissAddingDialog() {
        _state.update {
            it.copy(
                showAddingDialog = false
            )
        }
        getBooksJob?.cancel()
    }

    private fun requestAddingDialog() {
        _state.update {
            it.copy(
                showAddingDialog = true,
                selectedBooks = emptyList()
            )
        }
        onEvent(BrowseEvent.OnGetBooksFromFiles)
    }

    private fun getBooksFromFiles() {
        getBooksJob = viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    isBooksLoading = true
                )
            }

            yield()

            val books = mutableListOf<NullableBook>()
            _state.value.selectableFiles
                .filter { it.isSelected && !it.isDirectory }
                .ifEmpty {
                    _state.update {
                        it.copy(
                            isBooksLoading = false,
                            showAddingDialog = false
                        )
                    }
                    return@launch
                }
                .map { it.fileOrDirectory }
                .forEach {
                    yield()
                    books.add(getBookFromFile.execute(it))
                }

            yield()

            _state.update {
                it.copy(
                    selectedBooks = books.map { book -> book to true },
                    isBooksLoading = false
                )
            }
        }
    }

    private fun addBooks(event: BrowseEvent.OnAddBooks) {
        viewModelScope.launch {
            val booksToInsert = _state.value.selectedBooks
                .filter { it.first is NullableBook.NotNull }
                .filter { it.second }
                .map { it.first }

            if (booksToInsert.isEmpty()) {
                return@launch
            }

            var failed = false
            booksToInsert.forEach {
                if (!insertBook.execute(it.bookWithTextAndCover!!)) {
                    failed = true
                }
            }

            if (failed) {
                _state.update {
                    it.copy(
                        showAddingDialog = false
                    )
                }
                onEvent(BrowseEvent.OnLoadList)
                onEvent(BrowseEvent.OnClearSelectedFiles)
                event.onFailed()
                return@launch
            }

            event.resetScroll()
            event.onNavigate {
                navigate(Screen.Library)
            }
            event.onSuccess()

            _state.update {
                it.copy(
                    showAddingDialog = false
                )
            }
            onEvent(BrowseEvent.OnLoadList)
            onEvent(BrowseEvent.OnClearSelectedFiles)
        }
    }

    private fun loadList() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    isLoading = true,
                    hasSelectedItems = false,
                    listState = LazyListState(),
                    gridState = LazyGridState()
                )
            }
            getFilesFromDownloads()
        }
    }

    private fun updateScrollOffset() {
        _state.update {
            it.copy(
                listState = LazyListState(
                    it.listState.firstVisibleItemIndex,
                    0
                ),
                gridState = LazyGridState(
                    it.gridState.firstVisibleItemIndex,
                    0
                )
            )
        }
    }

    private fun changeDirectory(event: BrowseEvent.OnChangeDirectory) {
        viewModelScope.launch {
            changeDirectoryJob?.cancel()
            searchQueryJob?.cancel()

            changeDirectoryJob = launch(Dispatchers.IO) {
                yield()
                _state.update {
                    it.copy(
                        listState = LazyListState(),
                        gridState = LazyGridState(),
                        selectedDirectory = event.directory,
                        previousDirectory = if (event.savePreviousDirectory) it.selectedDirectory
                        else event.directory.parentFile,
                        inNestedDirectory = event.directory != Environment.getExternalStorageDirectory()
                    )
                }
            }
        }
    }

    private fun goBackDirectory() {
        onEvent(
            BrowseEvent.OnChangeDirectory(
                _state.value.previousDirectory
                    ?: Environment.getExternalStorageDirectory(),
                savePreviousDirectory = false
            )
        )
    }

    private fun toggleFilterBottomSheet() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    showFilterBottomSheet = !it.showFilterBottomSheet
                )
            }
        }
    }

    private fun scrollToFilterPage(event: BrowseEvent.OnScrollToFilterPage) {
        viewModelScope.launch {
            _state.update {
                event.pagerState?.scrollToPage(event.page)

                it.copy(
                    currentPage = event.page
                )
            }
        }
    }

    private fun updateFavoriteDirectory(event: BrowseEvent.OnUpdateFavoriteDirectory) {
        viewModelScope.launch {
            _state.update {
                it.copy(isLoading = true)
            }
            updateFavoriteDirectory.execute(event.path)
            getFilesFromDownloads()
        }
    }

    fun filterList(mainState: MainState): List<SelectableFile> {
        fun <T> thenCompareBy(
            selector: (T) -> Comparable<*>?
        ): Comparator<T> {
            return if (mainState.browseSortOrderDescending) {
                compareByDescending(selector)
            } else {
                compareBy(selector)
            }
        }

        fun List<SelectableFile>.filterFiles(): List<SelectableFile> {
            if (mainState.browseIncludedFilterItems.isEmpty()) {
                return this
            }

            return filter { file ->
                when (file.isDirectory) {
                    true -> {
                        return@filter this.filter {
                            if (file == it) {
                                return@filter false
                            }

                            it.fileOrDirectory.path.startsWith(file.fileOrDirectory.path)
                        }.filterFiles().isNotEmpty()
                    }

                    false -> {
                        return@filter mainState.browseIncludedFilterItems.any {
                            file.fileOrDirectory.path.endsWith(
                                it, ignoreCase = true
                            )
                        }
                    }
                }
            }
        }

        return _state.value.selectableFiles
            .filterFiles()
            .filter {
                if (
                    _state.value.hasSearched
                    || mainState.browseFilesStructure == BrowseFilesStructure.ALL_FILES
                ) {
                    return@filter !it.isDirectory
                }

                if (
                    Environment.getExternalStorageDirectory() == _state.value.selectedDirectory
                    && it.isFavorite
                    && mainState.browsePinFavoriteDirectories
                ) {
                    return@filter true
                }

                it.parentDirectory == _state.value.selectedDirectory
            }
            .sortedWith(
                compareByDescending<SelectableFile> {
                    when (mainState.browsePinFavoriteDirectories) {
                        true -> it.isFavorite
                        false -> true
                    }
                }.then(
                    compareByDescending {
                        when (mainState.browseSortOrder != BrowseSortOrder.FILE_TYPE) {
                            true -> it.isDirectory
                            false -> true
                        }
                    }
                ).then(
                    thenCompareBy {
                        when (mainState.browseSortOrder) {
                            BrowseSortOrder.NAME -> {
                                it.fileOrDirectory.name.lowercase().trim()
                            }

                            BrowseSortOrder.FILE_TYPE -> {
                                it.isDirectory
                            }

                            BrowseSortOrder.FILE_FORMAT -> {
                                it.fileOrDirectory.extension
                            }

                            BrowseSortOrder.FILE_SIZE -> {
                                it.fileOrDirectory.length()
                            }

                            BrowseSortOrder.LAST_MODIFIED -> {
                                it.fileOrDirectory.lastModified()
                            }
                        }
                    }
                )
            )
    }

    private suspend fun getFilesFromDownloads(
        query: String = if (_state.value.showSearch) _state.value.searchQuery else ""
    ) {
        getFilesFromDevice.execute(query).apply {
            yield()
            _state.update {
                it.copy(
                    selectableFiles = this,
                    selectedItemsCount = 0,
                    hasSearched = query.isNotBlank(),
                    hasSelectedItems = false,
                    isLoading = false
                )
            }
        }
    }

    fun clearViewModel() {
        viewModelScope.launch(Dispatchers.Main) {
            _state.update {
                it.copy(isError = false)
            }
        }
    }
}