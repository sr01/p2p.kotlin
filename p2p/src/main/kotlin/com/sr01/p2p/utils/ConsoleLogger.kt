package com.sr01.p2p.utils

/**
 * Created by Shmulik on 19/01/2018.
 * .
 */
object ConsoleLogger : Logger {
    override fun e(tag: String, msg: String, e: Throwable?) {
        println("[Error] [$tag] $msg ${if (e != null) "Exception: $e" else ""}")
    }

    override fun w(tag: String, msg: String, e: Throwable?) {
        println("[Warning] [$tag] $msg ${if (e != null) "Exception: $e" else ""}")
    }

    override fun i(tag: String, msg: String, e: Throwable?) {
        println("[Info] [$tag] $msg ${if (e != null) "Exception: $e" else ""}")
    }

    override fun d(tag: String, msg: String, e: Throwable?) {
        println("[Debug] [$tag] $msg ${if (e != null) "Exception: $e" else ""}")
    }

    override fun v(tag: String, msg: String, e: Throwable?) {
        println("[Verbose] [$tag] $msg ${if (e != null) "Exception: $e" else ""}")
    }
}
