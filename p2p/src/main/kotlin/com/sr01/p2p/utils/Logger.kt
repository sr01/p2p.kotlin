package com.sr01.p2p.utils

interface Logger {

    fun e(tag: String, msg: String, e: Throwable? = null)
    fun e(tag: String, msg: String) = e(tag, msg, null)

    fun w(tag: String, msg: String, e: Throwable? = null)
    fun w(tag: String, msg: String) = w(tag, msg, null)

    fun i(tag: String, msg: String, e: Throwable? = null)
    fun i(tag: String, msg: String) = i(tag, msg, null)

    fun d(tag: String, msg: String, e: Throwable? = null)
    fun d(tag: String, msg: String) = d(tag, msg, null)

    fun v(tag: String, msg: String, e: Throwable? = null)
    fun v(tag: String, msg: String) = v(tag, msg, null)

    fun write(level: LogLevels, tag: String, msg: String, e: Throwable? = null) =
            when (level) {
                Logger.LogLevels.ERROR -> e(tag, msg, e)
                Logger.LogLevels.WARN -> w(tag, msg, e)
                Logger.LogLevels.INFO -> i(tag, msg, e)
                Logger.LogLevels.DEBUG -> d(tag, msg, e)
                Logger.LogLevels.VERBOSE -> v(tag, msg, e)
            }

    enum class LogLevels {
        ERROR,
        WARN,
        INFO,
        DEBUG,
        VERBOSE
    }
}
