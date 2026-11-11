package com.manito.backlogquest.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LocalProfile(
    val displayName: String = "Jugador",
    val bio: String = "Cazando un juego cada vez.",
    val avatarUri: String? = null
)

class LocalProfileRepository(context: Context) {
    private val preferences = context.getSharedPreferences(
        "local_player_profile",
        Context.MODE_PRIVATE
    )

    private val _profile = MutableStateFlow(readProfile())
    val profile: StateFlow<LocalProfile> = _profile.asStateFlow()

    fun save(displayName: String, bio: String, avatarUri: String?) {
        val profile = LocalProfile(
            displayName = displayName.trim().take(24).ifBlank { "Jugador" },
            bio = bio.trim().take(120),
            avatarUri = avatarUri?.takeIf { it.isNotBlank() }
        )
        preferences.edit()
            .putString(KEY_DISPLAY_NAME, profile.displayName)
            .putString(KEY_BIO, profile.bio)
            .putString(KEY_AVATAR_URI, profile.avatarUri)
            .apply()
        _profile.value = profile
    }

    private fun readProfile() = LocalProfile(
        displayName = preferences.getString(KEY_DISPLAY_NAME, null)
            ?.takeIf { it.isNotBlank() }
            ?: "Jugador",
        bio = preferences.getString(KEY_BIO, null)
            ?: "Cazando un juego cada vez.",
        avatarUri = preferences.getString(KEY_AVATAR_URI, null)
    )

    private companion object {
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_BIO = "bio"
        const val KEY_AVATAR_URI = "avatar_uri"
    }
}
