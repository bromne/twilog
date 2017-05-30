package com.bromne.twilog.app

import android.content.SharedPreferences
import com.bromne.io.getSerializable
import com.bromne.io.putSerializable
import com.bromne.io.update
import com.bromne.twilog.client.TwilogClient
import com.google.common.collect.ImmutableSet
import org.joda.time.DateTime
import java.io.Serializable

val FAVORITES = "favorites"
val HISTORY = "history"

var SharedPreferences.favorites: ImmutableSet<SavedQuery>
    get() = this.getSerializable(FAVORITES) ?: ImmutableSet.of()
    set(value) = this.update { it.putSerializable(FAVORITES, value) }

var SharedPreferences.history: ImmutableSet<SavedQuery>
    get() = this.getSerializable(HISTORY) ?: ImmutableSet.of()
    set(value) = this.update { it.putSerializable(HISTORY, value) }

data class SavedQuery(val query: TwilogClient.Query, val created: DateTime) : Serializable {
    override fun equals(other: Any?): Boolean {
        return when (other) {
            is SavedQuery -> this.query == other.query
            else -> false
        }
    }

    override fun hashCode() = this.query.hashCode()
}


