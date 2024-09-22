package ua.acclorite.book_story.presentation.screens.settings.nested.reader.components.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ua.acclorite.book_story.R
import ua.acclorite.book_story.presentation.core.components.LocalMainViewModel
import ua.acclorite.book_story.presentation.data.MainEvent
import ua.acclorite.book_story.presentation.screens.settings.components.SwitchWithTitle
import ua.acclorite.book_story.presentation.ui.ExpandingTransition

/**
 * Check for Text Update Toast setting.
 * If true, shows a toast when the text is up-to-date.
 */
@Composable
fun CheckForTextUpdateToastSetting() {
    val state = LocalMainViewModel.current.state
    val onMainEvent = LocalMainViewModel.current.onEvent

    ExpandingTransition(visible = state.value.checkForTextUpdate) {
        SwitchWithTitle(
            selected = state.value.checkForTextUpdateToast,
            title = stringResource(id = R.string.check_for_text_update_toast_option),
            onClick = {
                onMainEvent(
                    MainEvent.OnChangeCheckForTextUpdateToast(
                        !state.value.checkForTextUpdateToast
                    )
                )
            }
        )
    }
}