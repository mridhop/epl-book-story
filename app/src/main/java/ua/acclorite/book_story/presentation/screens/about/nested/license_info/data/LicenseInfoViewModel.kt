package ua.acclorite.book_story.presentation.screens.about.nested.license_info.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ua.acclorite.book_story.presentation.data.Navigator
import javax.inject.Inject

@HiltViewModel
class LicenseInfoViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(LicenseInfoState())
    val state = _state.asStateFlow()

    fun onEvent(event: LicenseInfoEvent) {
        when (event) {
            is LicenseInfoEvent.OnOpenLicensePage -> {
                viewModelScope.launch {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(event.page)
                    )

                    if (intent.resolveActivity(event.context.packageManager) != null) {
                        event.context.startActivity(intent)
                        return@launch
                    }

                    event.noAppsFound()
                }
            }
        }
    }

    fun init(navigator: Navigator, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(license = null)
            }

            val licenseId = navigator.retrieveArgument("license") as? String

            if (licenseId == null) {
                navigator.navigateBack()
                return@launch
            }

            val license = Libs.Builder().withContext(context).build().libraries.find {
                it.uniqueId == licenseId
            }

            if (license == null) {
                navigator.navigateBack()
                return@launch
            }

            _state.update {
                it.copy(
                    license = license
                )
            }
        }
    }
}