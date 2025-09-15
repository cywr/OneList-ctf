package com.lolo.io.onelist

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lolo.io.onelist.core.data.crypto.CryptoUtils
import com.lolo.io.onelist.core.data.shared_preferences.SharedPreferencesHelper
import com.lolo.io.onelist.core.domain.use_cases.OneListUseCases
import com.lolo.io.onelist.core.model.FirstLaunchLists
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivityViewModel(
    private val firstLaunchLists: FirstLaunchLists,
    private val useCases: OneListUseCases,
    private val preferences: SharedPreferencesHelper,
    private val context: Context
) : ViewModel() {

    private val _showWhatsNew = MutableStateFlow(false)
    val showWhatsNew = _showWhatsNew.asStateFlow()
    private val _listsLoaded = MutableStateFlow(false)
    val listsLoaded = _listsLoaded.asStateFlow()

    fun init() {
        viewModelScope.launch {
            useCases.handleFirstLaunch(firstLaunchLists.firstLaunchLists())
            setAppVersion()
            useCases.loadAllLists().first()
            _listsLoaded.value = true
        }
    }

    private fun setAppVersion() {
        if (preferences.version != BuildConfig.VERSION_NAME) {
            _showWhatsNew.value = useCases.shouldShowWhatsNew(BuildConfig.VERSION_NAME)
            preferences.version = BuildConfig.VERSION_NAME
            
            // CTF Flag 10: Check if conditions are met for final flag
            checkFinalFlagConditions()
        }
    }
    
    private fun checkFinalFlagConditions() {
        // Check if previous flags have been found (simplified check)
        val hasFoundPreviousFlags = preferences.ctfFlag6 != null
        
        if (hasFoundPreviousFlags) {
            revealFinalFlag()
        }
    }
    
    private fun revealFinalFlag() {
        try {
            // Derive encryption key from app signature + device info
            val key = CryptoUtils.deriveKey(context)
            
            // Pre-encrypted flag (this would be pre-calculated with the expected key)
            // For demonstration, using a fallback approach
            val encryptedFlag = "EncryptedFlagDataHere" // This should be the actual encrypted data
            
            // Try to decrypt
            val decryptedFlag = CryptoUtils.decryptFlag(encryptedFlag, key)
            
            if (decryptedFlag != "decryption_failed") {
                Log.d("CTF_FINAL", "Ultimate flag: $decryptedFlag")
            } else {
                // Fallback: Generate flag with key hash for verification
                val keyHash = key.take(8)
                val finalFlag = "CYWR{crypto_master_final_$keyHash}"
                Log.d("CTF_FINAL", "Ultimate flag: $finalFlag")
                Log.d("CTF_FINAL", "Key material: $key")
                Log.d("CTF_FINAL", "Device fingerprint: ${CryptoUtils.deriveKey(context).take(16)}")
            }
        } catch (e: Exception) {
            Log.e("CTF_FINAL", "Flag generation failed", e)
        }
    }
}