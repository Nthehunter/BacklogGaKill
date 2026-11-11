package com.manito.backlogquest

import android.app.Application
import com.manito.backlogquest.data.AppDatabase
import com.manito.backlogquest.data.GameRepository
import com.manito.backlogquest.data.LocalProfileRepository
import com.manito.backlogquest.data.NintendoLibraryService

class BacklogQuestApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { GameRepository(database.gameDao()) }
    val profileRepository by lazy { LocalProfileRepository(this) }
    val nintendoLibraryService by lazy { NintendoLibraryService(this) }
}
