package com.bromne.twilog.client

import android.graphics.Bitmap
import com.bromne.data.Either
import org.joda.time.DateTime
import java.io.Serializable
import java.util.regex.Pattern


interface TwilogClient {
    fun find(query :Query): Result
    fun findRecent(userName: String): Result
    fun findByDate(userName: String): Result
    fun search(userName: String, query: String, joint: Joint): Result
    fun loadUserIcon(user: User): Bitmap

    data class Query(val userName: String, val body: Either<DateTime?, Criteria>, val order: Order) : Serializable

    data class Criteria(val keyword: String, val joint :Joint)

    enum class Joint {
        AND,
        OR;

        override fun toString() = if (this == Joint.AND) "a" else "o"
    }

    enum class Order {
        ASC,
        DESC
    }
}

fun String.extractWithPattern(pattern: Pattern): String? {
    val match = pattern.matcher(this)
    return if (match.find()) match.group(1) else null
}
