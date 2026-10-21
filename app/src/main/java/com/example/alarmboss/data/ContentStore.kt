package com.example.alarmboss.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "alarmboss_content")

/**
 * Holds user-supplied content for Medium-mode tasks. We deliberately do NOT ship any
 * copyrighted song lyrics or third-party text with the app -- the user types/pastes
 * their own reading passages and lyric lines here. (The barcode task needs no content: it
 * accepts any real-world barcode.)
 */
class ContentStore(private val context: Context) {

    private object Keys {
        val READING_PASSAGES = stringSetPreferencesKey("reading_passages")
        val LYRIC_LINES = stringSetPreferencesKey("lyric_lines")
        val REGISTERED_BARCODE = androidx.datastore.preferences.core.stringPreferencesKey("registered_barcode")
    }

    val readingPassages: Flow<List<String>> =
        context.dataStore.data.map { it[Keys.READING_PASSAGES]?.toList() ?: defaultPassages }

    val lyricLines: Flow<List<String>> =
        context.dataStore.data.map { it[Keys.LYRIC_LINES]?.toList() ?: emptyList() }

    val registeredBarcode: Flow<String?> =
        context.dataStore.data.map { it[Keys.REGISTERED_BARCODE] }

    suspend fun setReadingPassages(passages: List<String>) {
        context.dataStore.edit { it[Keys.READING_PASSAGES] = passages.toSet() }
    }

    suspend fun setLyricLines(lines: List<String>) {
        context.dataStore.edit { it[Keys.LYRIC_LINES] = lines.toSet() }
    }

    suspend fun setRegisteredBarcode(code: String?) {
        context.dataStore.edit {
            if (code.isNullOrBlank()) {
                it.remove(Keys.REGISTERED_BARCODE)
            } else {
                it[Keys.REGISTERED_BARCODE] = code.trim()
            }
        }
    }

    suspend fun getRandomPassage(): String = readingPassages.first().randomOrNull() ?: defaultPassages.first()
    suspend fun getRandomLyric(): String? = lyricLines.first().randomOrNull()
    suspend fun getRegisteredBarcode(): String? = registeredBarcode.first()

    companion object {
        // Inspiring morning wake-up passages used if the user hasn't added custom ones yet.
        val defaultPassages = listOf(
            "The morning sun rises steadily over the quiet hills, bringing calm clarity and a fresh start. Take a deep breath, drink a glass of water, and move forward into your day with focus and intention.",
            "Energy and momentum are built one conscious step at a time. Clear away the distractions of yesterday and direct your mind toward the goals that truly inspire you today."
        )
    }
}
