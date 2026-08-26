package com.adaptibot.common

object InterruptibleSleep {

    private const val SLICE_MS = 50L

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

