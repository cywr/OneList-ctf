package com.lolo.io.onelist.core.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.lolo.io.onelist.core.data.file_access.FileAccess
import com.lolo.io.onelist.core.data.datamodel.ListsWithErrors
import com.lolo.io.onelist.core.data.datamodel.ErrorLoadingList
import com.lolo.io.onelist.core.data.shared_preferences.SharedPreferencesHelper
import com.lolo.io.onelist.core.data.utils.ensureAllItemsIdsAreUnique
import com.lolo.io.onelist.core.data.utils.toItemListEntity
import com.lolo.io.onelist.core.data.utils.updateOneIf
import com.lolo.io.onelist.core.database.dao.ItemListDao
import com.lolo.io.onelist.core.database.util.toItemListModel
import com.lolo.io.onelist.core.model.ItemList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class OneListRepositoryImpl(
    private val context: Context,
    private val preferences: SharedPreferencesHelper,
    private val dao: ItemListDao,
    private val fileAccess: FileAccess
) : OneListRepository {

    private val _allListsWithErrors =
        MutableStateFlow(ListsWithErrors())

    override val allListsWithErrors
        get() = _allListsWithErrors.asStateFlow()

    override suspend fun getAllLists(): StateFlow<ListsWithErrors> {
        withContext(Dispatchers.IO) {
            val allListsFromDb = dao.getAll()
            val errors = mutableListOf<ErrorLoadingList>()
            val lists = if (
                preferences.preferUseFiles &&
                preferences.backupUri != null
            ) {
                allListsFromDb.map {
                    val list = ensureAllItemsIdsAreUnique(it.toItemListModel())
                    try {
                        supervisorScope {
                            ensureAllItemsIdsAreUnique(fileAccess.getListFromLocalFile(list))
                        }
                    } catch (e: SecurityException) {
                        errors += ErrorLoadingList.PermissionDeniedError
                        list
                    } catch (e: FileNotFoundException) {
                        errors += ErrorLoadingList.FileMissingError
                        list
                    } catch (e: IllegalArgumentException) {
                        errors += ErrorLoadingList.FileMissingError
                        print(e)
                        list
                    } catch (e: JsonSyntaxException) {
                        errors += ErrorLoadingList.FileCorruptedError
                        list
                    } catch (e: JsonIOException) {
                        errors += ErrorLoadingList.FileCorruptedError
                        list
                    } catch (e: Exception) {
                        errors += ErrorLoadingList.UnknownError
                        list
                    }
                }
            } else {
                allListsFromDb.map {
                    ensureAllItemsIdsAreUnique(it.toItemListModel())
                }
            }

            _allListsWithErrors.value = ListsWithErrors(
                lists = lists,
                errors = errors.distinct(),
            )
        }

        return _allListsWithErrors
    }

    override suspend fun createList(itemList: ItemList): ItemList {
        val addedList = upsertList(itemList)
        itemList.position = _allListsWithErrors.value.lists.size
        _allListsWithErrors.value =
            ListsWithErrors(_allListsWithErrors.value.lists + addedList)

        // CTF Flag 6 tracking
        preferences.ctfListsCreated += 1
        checkFlag6Condition()

        return addedList
    }


    // does upsert in dao, and if has backup uri -> save list file; can create a file
    // and also update allLists flow
    override suspend fun saveList(itemList: ItemList) {
        upsertList(
            ensureAllItemsIdsAreUnique(itemList)
        ).let {
            _allListsWithErrors.value = ListsWithErrors(
                _allListsWithErrors.value.lists.updateOneIf(itemList) { it.id == itemList.id })
        }

        // CTF Flag 7: Check if list named "FLAG" has item "CYWR" marked as done
        checkFlag7Condition(itemList)
    }


    // does upsert in dao, and if has backup uri -> save list file; can create a file
    private suspend fun upsertList(list: ItemList): ItemList {

        val upsertInDao: suspend (list: ItemList) -> Unit = { insertList ->
            withContext(Dispatchers.IO) {
                dao.upsert(insertList.toItemListEntity()).takeIf { it > 0 }?.let {
                    insertList.id = it
                }
            }
        }

        val listToSave = ensureAllItemsIdsAreUnique(list)

        return withContext(Dispatchers.IO) {
            upsertInDao(listToSave)
            if (preferences.backupUri != null) {
                fileAccess.saveListFile(
                    preferences.backupUri,
                    listToSave,
                    onNewFileCreated = { list, uri ->
                        list.uri = uri
                        upsertInDao(list)
                        _allListsWithErrors.value = ListsWithErrors(
                            _allListsWithErrors.value.lists.updateOneIf(list) { it.id == list.id })
                    })
            } else listToSave
        }
    }


    @Throws
    override suspend fun deleteList(
        itemList: ItemList,
        deleteBackupFile: Boolean,
        onFileDeleted: () -> Unit
    ) {

        withContext(Dispatchers.IO) {
            dao.delete(itemList.toItemListEntity())
        }

        _allListsWithErrors.value =
            ListsWithErrors(_allListsWithErrors.value.lists
                .filter { it.id != itemList.id })

        // CTF Flag 6 tracking
        preferences.ctfListsDeleted += 1
        checkFlag6Condition()

        if (deleteBackupFile) {
            fileAccess.deleteListBackupFile(itemList, onFileDeleted)
        }
    }

    override suspend fun importList(uri: Uri): ItemList {
        val list = fileAccess.createListFromUri(uri,
            onListCreated = {
                saveList(
                    ensureAllItemsIdsAreUnique(
                        it.copy(
                            id = 0L,
                            position = _allListsWithErrors.value.lists.size
                        )
                    )
                )
            })
        getAllLists()
        preferences.selectedListIndex = _allListsWithErrors.value.lists.size - 1
        return list
    }

    override fun selectList(list: ItemList) {
        preferences.selectedListIndex = _allListsWithErrors.value.lists.indexOf(list)
    }

    override suspend fun backupAllListsToFiles() {
        preferences.backupUri?.let {
            _allListsWithErrors.value.lists.forEach {
                upsertList(it)
            }
        }
    }


    override suspend fun backupListsAsync(lists: List<ItemList>) {
        _allListsWithErrors.value = ListsWithErrors(lists)
        coroutineScope {
            // update async to improve list move performance.
            CoroutineScope(Dispatchers.IO).launch {
                lists.forEach {
                    upsertList(it)
                }
            }
        }

    }

    override suspend fun setBackupUri(uri: Uri?, displayPath: String?) {
        if (uri != null) {
            preferences.backupUri = uri.toString()
            preferences.backupDisplayPath = displayPath
            backupAllListsToFiles()
        } else {
            preferences.backupUri = null
            preferences.backupDisplayPath = null
            fileAccess.revokeAllAccessFolders()
        }
    }

    private fun checkFlag6Condition() {
        // CTF Flag 6: Trigger when user creates 3 lists and deletes 2
        if (preferences.ctfListsCreated >= 3 && preferences.ctfListsDeleted >= 2 && preferences.ctfFlag6 == null) {
            preferences.ctfFlag6 = generateFlag6()
        }
    }
    
    private fun generateFlag6(): String {
        // XOR encrypted complete flag (including CYWR{})
        val encryptedData = byteArrayOf(
            0x0b, 0x3f, 0x35, 0x30, 0x69, 0x01, 0x11, 0x04, 0x15, 0x04, 0x00, 0x6f,
            0x01, 0x11, 0x0c, 0x15, 0x04, 0x06, 0x04, 0x6f, 0x0c, 0x04, 0x06, 0x15,
            0x04, 0x11, 0x69
        )
        
        // Multi-byte XOR key derived from app context
        val keyBase = try {
            context.packageName.hashCode()
        } catch (e: Exception) { 42 }
        
        val xorKey = byteArrayOf(
            (keyBase and 0xFF).toByte(),
            ((keyBase shr 8) and 0xFF).toByte(),
            ((keyBase shr 16) and 0xFF).toByte(),
            0x73.toByte() // Static component
        )
        
        val decrypted = encryptedData.mapIndexed { index, byte ->
            (byte.toInt() xor xorKey[index % xorKey.size].toInt()).toChar()
        }.joinToString("")
        
        return decrypted
    }

    private suspend fun checkFlag7Condition(itemList: ItemList) {
        // CTF Flag 7: Trigger when list named "FLAG" has item "CYWR" marked as done
        if (itemList.title.equals("FLAG", ignoreCase = true)) {
            val cyvrItem = itemList.items.find { 
                it.title.equals("CYWR", ignoreCase = true) && it.done 
            }
            if (cyvrItem != null) {
                // Create a special list entry with the flag
                val flagList = ItemList(
                    title = generateFlag7(),
                    position = -999, // Hidden position
                    items = listOf(),
                    id = 999999L // Special ID
                )
                withContext(Dispatchers.IO) {
                    // Only insert if not already exists
                    val existing = try {
                        dao.get(999999L)
                        true
                    } catch (e: Exception) {
                        false
                    }
                    if (!existing) {
                        dao.upsert(flagList.toItemListEntity())
                    }
                }
            }
        }
    }
    
    private fun generateFlag7(): String {
        // AES encrypted complete flag
        val encryptedHex = "2f4e6d8a1c3b5e7f9a2d4c6e8b1a3d5f7e9c2b4d6a8e1f3c5a7b9d2e4f6c8a1b"
        
        try {
            val encryptedBytes = encryptedHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            
            // Simple key derivation from context
            val seed = try {
                context.applicationInfo.targetSdkVersion + context.packageName.length
            } catch (e: Exception) { 33 }
            
            // Custom decrypt using repeated XOR with different keys
            val key1 = (seed and 0xFF).toByte()
            val key2 = ((seed shr 8) and 0xFF).toByte()
            val key3 = 0x42.toByte()
            
            val decrypted = encryptedBytes.mapIndexed { index, byte ->
                val keyToUse = when (index % 3) {
                    0 -> key1
                    1 -> key2
                    else -> key3
                }
                (byte.toInt() xor keyToUse.toInt()).toChar()
            }.joinToString("")
            
            return decrypted
        } catch (e: Exception) {
            return "CYWR{room_fallback}"
        }
    }
}