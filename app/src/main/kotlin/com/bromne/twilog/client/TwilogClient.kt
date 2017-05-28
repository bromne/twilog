package com.bromne.twilog.client

import android.graphics.Bitmap
import java.util.regex.Pattern

interface TwilogClient {
    fun findRecent(userName: String): Result
    fun findByDate(userName: String): Result
    fun search(userName: String, query: String, joint: Joint): Result
    fun loadUserIcon(user: User): Bitmap

    enum class Joint {
        AND,
        OR;

        override fun toString() = if (this == Joint.AND) "a" else "o"
    }
}

fun String.extractWithPattern(pattern: Pattern): String? {
    val match = pattern.matcher(this)
    return if (match.find()) match.group(1) else null
}


