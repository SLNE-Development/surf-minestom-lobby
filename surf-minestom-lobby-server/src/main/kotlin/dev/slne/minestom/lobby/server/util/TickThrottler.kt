package dev.slne.minestom.lobby.server.util

import java.util.concurrent.atomic.AtomicInteger

class TickThrottler(private val incrementStep: Int, private val threshold: Int) {

    private val count = AtomicInteger()

    fun increment() {
        count.addAndGet(incrementStep)
    }

    fun tick() {
        var value: Int
        do {
            value = count.get()
            if (value <= 0) return
        } while (!count.compareAndSet(value, value - 1))
    }

    fun isUnderThreshold() = threshold <= 0 || count.get() < threshold

    fun isIncrementAndUnderThreshold() = count.addAndGet(incrementStep) < threshold
}
