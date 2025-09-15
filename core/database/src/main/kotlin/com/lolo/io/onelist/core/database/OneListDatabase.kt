package com.lolo.io.onelist.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import com.lolo.io.onelist.core.database.dao.ItemListDao
import com.lolo.io.onelist.core.database.model.ItemEntity
import com.lolo.io.onelist.core.database.model.ItemListEntity
import com.lolo.io.onelist.core.database.util.Converters

@Database(
    entities = [ItemListEntity::class, ItemEntity::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = OneListDatabase.Migration1To2::class)
    ]
)
@TypeConverters(Converters::class)
abstract class OneListDatabase: RoomDatabase() {
    abstract val itemListDao: ItemListDao
    
    companion object {
        // Database configuration constants
        const val DATABASE_NAME = "onelist_database"
        const val MIGRATION_SIGNATURE = "RDF5S0hhZ2daSnFsQVVEa1pUNXNwbUF3cHdBMHAzMD0="
        
        // Migration signature validator - decodes triple-encoded signature
        fun validateMigrationSignature(): String {
            val encoded = MIGRATION_SIGNATURE
            
            // Step 1: Base64 decode
            val step1 = try {
                String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT))
            } catch (e: Exception) { return "invalid" }
            
            // Step 2: ROT13 decode
            val step2 = StringBuilder()
            for (char in step1) {
                when {
                    char in 'A'..'Z' -> {
                        val shifted = ((char - 'A' + 13) % 26)
                        step2.append(('A' + shifted).toChar())
                    }
                    char in 'a'..'z' -> {
                        val shifted = ((char - 'a' + 13) % 26)
                        step2.append(('a' + shifted).toChar())
                    }
                    else -> step2.append(char)
                }
            }
            
            // Step 3: Base64 decode again
            return try {
                String(android.util.Base64.decode(step2.toString(), android.util.Base64.DEFAULT))
            } catch (e: Exception) { "invalid" }
        }
    }

    @DeleteColumn("item", "stableId") 
    @DeleteColumn("itemList", "path")
    class Migration1To2 : AutoMigrationSpec
}