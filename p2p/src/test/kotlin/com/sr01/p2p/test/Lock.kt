package com.sr01.p2p.test

class Lock {
    private val lock = java.lang.Object()

    fun lockAndWait(timeout: Long) {
        synchronized(lock) {
            lock.wait(timeout)
        }
    }

    fun release() {
        synchronized(lock) {
            lock.notify()
        }
    }
}