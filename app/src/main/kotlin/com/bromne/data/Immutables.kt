package com.bromne.data

import com.google.common.collect.ImmutableSet

fun <T> ImmutableSet<T>.toBuilder() = ImmutableSet.builder<T>().addAll(this)

