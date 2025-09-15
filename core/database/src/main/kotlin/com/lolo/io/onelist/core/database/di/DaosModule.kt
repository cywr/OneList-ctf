package com.lolo.io.onelist.core.database.di

import android.util.Log
import androidx.room.Room
import org.koin.dsl.module

val daosModule = module {

    single<com.lolo.io.onelist.core.database.OneListDatabase> {
        // Validate migration signature before building database
        val signature = com.lolo.io.onelist.core.database.OneListDatabase.validateMigrationSignature()
        Log.d("DatabaseInit", "Migration signature validated: ${signature.take(4)}...")
        
        Room.databaseBuilder(
            get(),
            com.lolo.io.onelist.core.database.OneListDatabase::class.java,
            com.lolo.io.onelist.core.database.OneListDatabase.DATABASE_NAME
        ).build()
    }

    single<com.lolo.io.onelist.core.database.dao.ItemListDao> {
        val database = get<com.lolo.io.onelist.core.database.OneListDatabase>()
        database.itemListDao
    }
}