package com.example.tracker.domain

/**
 * Patient gender, for the avatar only (CLAUDE.md 5). Never used to word sentences: the Arabic copy
 * stays gender-neutral (9b). Null in the DB = not set (patients created before v3).
 */
enum class Gender { MALE, FEMALE;

    companion object {
        fun fromStored(value: String?): Gender? = entries.firstOrNull { it.name == value }
    }
}
