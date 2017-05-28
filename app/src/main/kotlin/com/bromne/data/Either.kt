package com.bromne.data

import java.io.Serializable

sealed class Either<L, R> : Serializable {
    val isLeft: Boolean get() {
        return when (this) {
            is Left -> true
            is Right -> false
        }
    }

    val isRight: Boolean get() = !this.isLeft

    fun <T> map(leftMapper: (L) -> T, rightMapper: (R) -> T): T {
        return when (this) {
            is Left -> leftMapper(this.value)
            is Right -> rightMapper(this.value)
        }
    }

    companion object {
        fun <L, R> left(value: L): Either<L, R> = Left(value)
        fun <L, R> right(value: R): Either<L, R> = Right(value)
    }

    class Left<L, R>(val value: L) : Either<L, R>()
    class Right<L, R>(val value: R) : Either<L, R>()
}
