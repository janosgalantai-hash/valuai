package com.valuai.network

import android.content.Context

object ImageRepository {
    private const val PREFS_NAME = "valuai_image_map"

    fun saveImages(context: Context, estimationId: String, paths: List<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(estimationId, paths.joinToString("|"))
            .apply()
    }

    fun getImages(context: Context, estimationId: String): List<String> {
        val stored = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(estimationId, "") ?: ""
        return if (stored.isBlank()) emptyList() else stored.split("|").filter { it.isNotBlank() }
    }
}
