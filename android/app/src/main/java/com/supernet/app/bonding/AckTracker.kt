package com.supernet.app.bonding

class AckTracker(initialSequence: Long = 0L) {
    private var contiguous = initialSequence - 1
    private val seen = java.util.TreeSet<Long>()

    @Synchronized
    fun observe(sequence: Long): Long {
        if (sequence <= contiguous) return contiguous
        seen.add(sequence)
        while (seen.remove(contiguous + 1)) contiguous++
        return contiguous
    }

    @Synchronized
    fun highestContiguous(): Long = contiguous

    @Synchronized
    fun reset(firstSequence: Long = 0L) {
        contiguous = firstSequence - 1
        seen.clear()
    }
}
