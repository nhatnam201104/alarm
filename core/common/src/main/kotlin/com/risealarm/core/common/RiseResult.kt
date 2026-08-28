package com.risealarm.core.common

sealed interface RiseResult<out T> {
    data class Success<T>(val value: T) : RiseResult<T>
    data class Error(val message: String) : RiseResult<Nothing>
}

