package com.valuai.network

import android.content.Context
import java.io.File

object ImageRepository {
    private const val PREFS_NAME = "valuai_image_map"
    private const val MAX_STORED = 30

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

    fun cleanup(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val all = prefs.all
        if (all.size <= MAX_STORED) return

        // Sort by key (estimation IDs are UUIDs — sort by insertion order via prefs isn't guaranteed,
        // so we delete all but the last MAX_STORED by entry count)
        val toDelete = all.keys.drop(MAX_STORED)
        val editor = prefs.edit()
        for (key in toDelete) {
            val paths = (all[key] as? String)?.split("|") ?: continue
            for (path in paths) {
                val file = File(path)
                if (file.exists()) file.delete()
            }
            editor.remove(key)
        }
        editor.apply()
    }
}
