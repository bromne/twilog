package com.bromne.twilog.client

fun <T> Iterable<T?>.excludeNullable(): Iterable<T> {
    return this.filter { it != null}
            .map { it!! }
}
