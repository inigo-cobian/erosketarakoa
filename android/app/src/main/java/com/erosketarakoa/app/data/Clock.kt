package com.erosketarakoa.app.data

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Abstraction over time and id generation so repositories are deterministic under test. */
interface Clock {
    fun nowMillis(): Long
    fun newId(): String
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun newId(): String = UUID.randomUUID().toString()
}
