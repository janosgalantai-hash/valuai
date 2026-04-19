package com.valuai

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppEventBus {
    private val _unauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorized = _unauthorized.asSharedFlow()

    fun emitUnauthorized() { _unauthorized.tryEmit(Unit) }
}
