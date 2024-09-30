package ua.acclorite.book_story.presentation.data

import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.acclorite.book_story.domain.use_case.ChangeLanguage
import ua.acclorite.book_story.domain.use_case.CheckForUpdates
import ua.acclorite.book_story.domain.use_case.GetAllSettings
import ua.acclorite.book_story.domain.use_case.SetDatastore
import ua.acclorite.book_story.presentation.core.constants.Constants
import ua.acclorite.book_story.presentation.core.constants.DataStoreConstants
import ua.acclorite.book_story.presentation.core.util.BaseViewModel
import ua.acclorite.book_story.presentation.screens.library.data.LibraryViewModel
import ua.acclorite.book_story.presentation.screens.settings.data.SettingsViewModel
import ua.acclorite.book_story.presentation.screens.settings.nested.browse.data.toBrowseLayout
import ua.acclorite.book_story.presentation.screens.settings.nested.browse.data.toBrowseSortOrder
import ua.acclorite.book_story.presentation.screens.settings.nested.browse.data.toFilesStructure
import ua.acclorite.book_story.presentation.screens.settings.nested.reader.data.toReaderScreenOrientation
import ua.acclorite.book_story.presentation.screens.settings.nested.reader.data.toTextAlignment
import ua.acclorite.book_story.presentation.ui.toDarkTheme
import ua.acclorite.book_story.presentation.ui.toPureDark
import ua.acclorite.book_story.presentation.ui.toTheme
import ua.acclorite.book_story.presentation.ui.toThemeContrast
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val stateHandle: SavedStateHandle,
    private val setDatastore: SetDatastore,
    private val changeLanguage: ChangeLanguage,
    private val checkForUpdates: CheckForUpdates,
    private val getAllSettings: GetAllSettings
) : BaseViewModel<MainState, MainEvent>() {

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private val isSettingsReady = MutableStateFlow(false)
    private val isViewModelReady = MutableStateFlow(false)

    private val _state: MutableStateFlow<MainState> = MutableStateFlow(
        stateHandle[Constants.MAIN_STATE] ?: MainState()
    )
    override val state = _state.asStateFlow()

    override fun onEvent(event: MainEvent) {
        when (event) {
            is MainEvent.OnChangeLanguage -> handleLanguageChange(event.lang)
            is MainEvent.OnChangeDarkTheme -> handleDatastoreUpdate(DataStoreConstants.DARK_THEME, event.darkTheme) { it.copy(darkTheme = event.darkTheme.toDarkTheme()) }
            is MainEvent.OnChangePureDark -> handleDatastoreUpdate(DataStoreConstants.PURE_DARK, event.pureDark) { it.copy(pureDark = event.pureDark.toPureDark()) }
            is MainEvent.OnChangeThemeContrast -> handleDatastoreUpdate(DataStoreConstants.THEME_CONTRAST, event.themeContrast) { it.copy(themeContrast = event.themeContrast.toThemeContrast()) }
            is MainEvent.OnChangeTheme -> handleDatastoreUpdate(DataStoreConstants.THEME, event.theme) { it.copy(theme = event.theme.toTheme()) }
            is MainEvent.OnChangeFontFamily -> handleDatastoreUpdate(DataStoreConstants.FONT, event.fontFamily) { it.copy(fontFamily = Constants.FONTS.find { font -> font.id == event.fontFamily }?.id ?: Constants.FONTS[0].id) }
            is MainEvent.OnChangeFontStyle -> handleDatastoreUpdate(DataStoreConstants.IS_ITALIC, event.fontStyle) { it.copy(isItalic = event.fontStyle) }
            is MainEvent.OnChangeFontSize -> handleDatastoreUpdate(DataStoreConstants.FONT_SIZE, event.fontSize) { it.copy(fontSize = event.fontSize) }
            is MainEvent.OnChangeLineHeight -> handleDatastoreUpdate(DataStoreConstants.LINE_HEIGHT, event.lineHeight) { it.copy(lineHeight = event.lineHeight) }
            is MainEvent.OnChangeParagraphHeight -> handleDatastoreUpdate(DataStoreConstants.PARAGRAPH_HEIGHT, event.paragraphHeight) { it.copy(paragraphHeight = event.paragraphHeight) }
            is MainEvent.OnChangeParagraphIndentation -> handleDatastoreUpdate(DataStoreConstants.PARAGRAPH_INDENTATION, event.indentation) { it.copy(paragraphIndentation = event.indentation) }
            is MainEvent.OnChangeShowStartScreen -> handleDatastoreUpdate(DataStoreConstants.SHOW_START_SCREEN, event.bool) { it.copy(showStartScreen = event.bool) }
            is MainEvent.OnChangeCheckForUpdates -> handleDatastoreUpdate(DataStoreConstants.CHECK_FOR_UPDATES, event.bool) { it.copy(checkForUpdates = event.bool) }
            is MainEvent.OnChangeSidePadding -> handleDatastoreUpdate(DataStoreConstants.SIDE_PADDING, event.sidePadding) { it.copy(sidePadding = event.sidePadding) }
            is MainEvent.OnChangeDoubleClickTranslation -> handleDatastoreUpdate(DataStoreConstants.DOUBLE_CLICK_TRANSLATION, event.bool) { it.copy(doubleClickTranslation = event.bool) }
            is MainEvent.OnChangeFastColorPresetChange -> handleDatastoreUpdate(DataStoreConstants.FAST_COLOR_PRESET_CHANGE, event.bool) { it.copy(fastColorPresetChange = event.bool) }
            is MainEvent.OnChangeBrowseFilesStructure -> handleDatastoreUpdate(DataStoreConstants.BROWSE_FILES_STRUCTURE, event.structure) { it.copy(browseFilesStructure = event.structure.toFilesStructure()) }
            is MainEvent.OnChangeBrowseLayout -> handleDatastoreUpdate(DataStoreConstants.BROWSE_LAYOUT, event.layout) { it.copy(browseLayout = event.layout.toBrowseLayout()) }
            is MainEvent.OnChangeBrowseAutoGridSize -> handleDatastoreUpdate(DataStoreConstants.BROWSE_AUTO_GRID_SIZE, event.bool) { it.copy(browseAutoGridSize = event.bool) }
            is MainEvent.OnChangeBrowseGridSize -> handleDatastoreUpdate(DataStoreConstants.BROWSE_GRID_SIZE, event.size) { it.copy(browseGridSize = event.size) }
            is MainEvent.OnChangeBrowsePinFavoriteDirectories -> handleDatastoreUpdate(DataStoreConstants.BROWSE_PIN_FAVORITE_DIRECTORIES, event.bool) { it.copy(browsePinFavoriteDirectories = event.bool) }
            is MainEvent.OnChangeBrowseSortOrder -> handleDatastoreUpdate(DataStoreConstants.BROWSE_SORT_ORDER, event.order) { it.copy(browseSortOrder = event.order.toBrowseSortOrder()) }
            is MainEvent.OnChangeBrowseSortOrderDescending -> handleDatastoreUpdate(DataStoreConstants.BROWSE_SORT_ORDER_DESCENDING, event.bool) { it.copy(browseSortOrderDescending = event.bool) }
            is MainEvent.OnChangeBrowseIncludedFilterItem -> handleBrowseIncludedFilterItem(event.item)
            is MainEvent.OnChangeTextAlignment -> handleDatastoreUpdate(DataStoreConstants.TEXT_ALIGNMENT, event.alignment) { it.copy(textAlignment = event.alignment.toTextAlignment()) }
            is MainEvent.OnChangeDoublePressExit -> handleDatastoreUpdate(DataStoreConstants.DOUBLE_PRESS_EXIT, event.bool) { it.copy(doublePressExit = event.bool) }
            is MainEvent.OnChangeLetterSpacing -> handleDatastoreUpdate(DataStoreConstants.LETTER_SPACING, event.spacing) { it.copy(letterSpacing = event.spacing) }
            is MainEvent.OnChangeAbsoluteDark -> handleDatastoreUpdate(DataStoreConstants.ABSOLUTE_DARK, event.bool) { it.copy(absoluteDark = event.bool) }
            is MainEvent.OnChangeCutoutPadding -> handleDatastoreUpdate(DataStoreConstants.CUTOUT_PADDING, event.bool) { it.copy(cutoutPadding = event.bool) }
            is MainEvent.OnChangeFullscreen -> handleDatastoreUpdate(DataStoreConstants.FULLSCREEN, event.bool) { it.copy(fullscreen = event.bool) }
            is MainEvent.OnChangeKeepScreenOn -> handleDatastoreUpdate(DataStoreConstants.KEEP_SCREEN_ON, event.bool) { it.copy(keepScreenOn = event.bool) }
            is MainEvent.OnChangeVerticalPadding -> handleDatastoreUpdate(DataStoreConstants.VERTICAL_PADDING, event.padding) { it.copy(verticalPadding = event.padding) }
            is MainEvent.OnChangeHideBarsOnFastScroll -> handleDatastoreUpdate(DataStoreConstants.HIDE_BARS_ON_FAST_SCROLL, event.bool) { it.copy(hideBarsOnFastScroll = event.bool) }
            is MainEvent.OnChangePerceptionExpander -> handleDatastoreUpdate(DataStoreConstants.PERCEPTION_EXPANDER, event.bool) { it.copy(perceptionExpander = event.bool) }
            is MainEvent.OnChangePerceptionExpanderPadding -> handleDatastoreUpdate(DataStoreConstants.PERCEPTION_EXPANDER_PADDING, event.padding) { it.copy(perceptionExpanderPadding = event.padding) }
            is MainEvent.OnChangePerceptionExpanderThickness -> handleDatastoreUpdate(DataStoreConstants.PERCEPTION_EXPANDER_THICKNESS, event.thickness) { it.copy(perceptionExpanderThickness = event.thickness) }
            is MainEvent.OnChangeCheckForTextUpdate -> handleDatastoreUpdate(DataStoreConstants.CHECK_FOR_TEXT_UPDATE, event.bool) { it.copy(checkForTextUpdate = event.bool) }
            is MainEvent.OnChangeCheckForTextUpdateToast -> handleDatastoreUpdate(DataStoreConstants.CHECK_FOR_TEXT_UPDATE_TOAST, event.bool) { it.copy(checkForTextUpdateToast = event.bool) }
            is MainEvent.OnChangeScreenOrientation -> handleDatastoreUpdate(DataStoreConstants.SCREEN_ORIENTATION, event.orientation) { it.copy(screenOrientation = event.orientation.toReaderScreenOrientation()) }
        }
    }

    private fun handleLanguageChange(lang: String) {
        viewModelScope.launch(Dispatchers.Main) {
            changeLanguage.execute(lang)
            updateStateWithSavedHandle { it.copy(language = lang) }
        }
    }

    private fun <T> handleDatastoreUpdate(key: Preferences.Key<T>, value: T, updateState: (MainState) -> MainState) {
        viewModelScope.launch(Dispatchers.IO) {
            setDatastore.execute(key, value)
            updateStateWithSavedHandle(updateState)
        }
    }

    private fun handleBrowseIncludedFilterItem(item: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val set = _state.value.browseIncludedFilterItems.toMutableSet()
            if (!set.add(item)) {
                set.remove(item)
            }
            setDatastore.execute(DataStoreConstants.BROWSE_INCLUDED_FILTER_ITEMS, set)
            updateStateWithSavedHandle { it.copy(browseIncludedFilterItems = set.toList()) }
        }
    }

    fun init(libraryViewModel: LibraryViewModel, settingsViewModel: SettingsViewModel) {
        viewModelScope.launch(Dispatchers.Main) {
            val settings = getAllSettings.execute()
            changeLanguage.execute(settings.language)
            if (settings.checkForUpdates) {
                viewModelScope.launch(Dispatchers.IO) {
                    checkForUpdates.execute(postNotification = true)
                }
            }
            updateStateWithSavedHandle { settings }
            isSettingsReady.update { true }
        }

        viewModelScope.launch(Dispatchers.IO) {
            combine(libraryViewModel.isReady, settingsViewModel.isReady) { (libraryViewModelReady, settingsViewModelReady) ->
                libraryViewModelReady && settingsViewModelReady
            }.collectLatest { ready ->
                isViewModelReady.update { ready }
            }
        }

        val isReady = combine(isViewModelReady, isSettingsReady) { values -> values.all { it } }
        viewModelScope.launch(Dispatchers.IO) {
            isReady.first { bool ->
                if (bool) {
                    _isReady.update { true }
                }
                bool
            }
        }
    }

    private fun updateStateWithSavedHandle(function: (MainState) -> MainState) {
        _state.update {
            stateHandle[Constants.MAIN_STATE] = function(it)
            function(it)
        }
    }
}