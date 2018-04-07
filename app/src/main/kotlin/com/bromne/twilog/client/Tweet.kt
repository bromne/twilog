package com.bromne.twilog.client

import org.joda.time.LocalDateTime
import java.io.Serializable
import java.text.MessageFormat
import java.util.regex.Pattern

data class Tweet(val status: String, val user: User, val created: LocalDateTime, val message: String, val raw: String, val isRetweet: Boolean) {
    val id: Long = this.status.extractWithPattern(Tweet.idPattern)
            .let { java.lang.Long.parseLong(it) }

    override fun toString(): String = MessageFormat.format("[{0} {1}] {2}", this.created.toString("yyyy-MM-dd HH:mm:ss"), this.user.name, this.message)

    companion object {
        val idPattern = Pattern.compile("https://twitter.com/.+/status/(\\d+)")!!
    }
}

data class User(val name: String, val display: String, val image: UserImage) : Serializable

data class UserImage(val base: String, val extension: String) : Serializable {
    val original: String get() = this.base + this.extension
    val bigger: String get() = this.base + "_bigger" + this.extension
    val normal: String get() = this.base + "_normal" + this.extension
    val mini: String get() = this.base + "_mini" + this.extension

    companion object {
        val PATTERN_NORMAL = Pattern.compile("(.+)_normal(\\..+|)")!!
        val PATTERN_BIGGER = Pattern.compile("(.+)_bigger(\\..+|)")!!

        fun fromNormal(url: String) = fromPattern(url, PATTERN_NORMAL)
        fun fromBigger(url: String) = fromPattern(url, PATTERN_BIGGER)

        internal fun fromPattern(url: String, pattern: Pattern): UserImage {
            val match = pattern.matcher(url)
            if (match.find()) {
                val base = match.group(1)
                val extension = match.group(2)
                return UserImage(base, extension)
            } else {
                throw IllegalArgumentException()
            }
        }
    }

}

data class Result(val user: User, val tweets: List<Tweet?>, val hasNext: Boolean)
