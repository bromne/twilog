package com.bromne.twilog.client

import android.graphics.Bitmap
import com.bromne.stereotypes.data.Either
import org.joda.time.DateTime
import org.joda.time.LocalDate
import java.io.Serializable
import java.util.regex.Pattern


interface TwilogClient {
    fun find(query :Query): Result

    fun loadUserIcon(url: String): Bitmap

    fun forceUpdate(user: User): Unit

    data class Query(val userName: String, val body: Either<LocalDate?, Criteria>, val order: Order) : Serializable

    data class Criteria(val keyword: String, val joint :Joint, val page: Int? = null) : Serializable

    enum class Joint {
        AND,
        OR;

        override fun toString() = if (this == Joint.AND) "a" else "o"
    }

    enum class Order {
        ASC,
        DESC;

        val reversed: Order
            get() = if (this == ASC) DESC else ASC
    }
}

fun String.extractWithPattern(pattern: Pattern): String? {
    val match = pattern.matcher(this)
    return if (match.find()) match.group(1) else null
}
