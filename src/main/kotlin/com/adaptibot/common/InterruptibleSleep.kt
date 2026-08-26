package com.adaptibot.common

/**
 * Sleep that reacts to thread interruption as fast as possible.
 *
 * Long waits are split into short slices, so a stop request (thread interrupt) is honoured
 * quickly regardless of where the waiting happens.
 *
 * The interruption is **never swallowed**: the interrupt flag is restored and
 * [InterruptedException] is propagated, so the execution loop can unwind immediately.
 */
object InterruptibleSleep {

    private const val SLICE_MS = 50L

    /**
     * Sleeps for [millis] milliseconds. Values `<= 0` only check the interrupt flag.
     *
     * @throws InterruptedException as soon as the current thread gets interrupted.
     */
    @Throws(InterruptedException::class)
    fun sleep(millis: Long) {
        if (millis <= 0) {
            checkInterrupted()
            return
        }

        val deadline = System.nanoTime() + millis * 1_000_000
        while (true) {
            checkInterrupted()
            val remainingMs = (deadline - System.nanoTime()) / 1_000_000
            if (remainingMs <= 0) return
            Thread.sleep(minOf(remainingMs, SLICE_MS))
        }
    }

    private fun checkInterrupted() {
        if (Thread.currentThread().isInterrupted) throw InterruptedException("Sleep interrupted")
    }
}

