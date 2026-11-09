package com.isai.player.ui.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isai.player.data.repository.SettingsRepository
import com.isai.player.data.scanner.LibraryScanner
import com.isai.player.data.scanner.ScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val libraryScanner: LibraryScanner,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val scanState: StateFlow<ScanState> = libraryScanner.scanState

    fun onFolderSelected(treeUri: Uri) {
        try {
            val contentResolver = context.contentResolver
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(treeUri, takeFlags)

            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val folderName = docId.substringAfterLast(':', docId).substringAfterLast('/')
            val displayName = if (folderName.isBlank()) "Music" else folderName

            viewModelScope.launch {
                settingsRepository.setSelectedFolder(treeUri.toString(), displayName)
                libraryScanner.startScan(treeUri)
            }
        } catch (e: Exception) {
            // Permission grant failure or invalid URI
        }
    }
}

