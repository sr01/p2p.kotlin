package com.sr01.p2p.utils

import java.io.PrintWriter
import java.io.StringWriter

object ThrowableUtils {
    fun printStackTrace(prefix: String, t: Throwable) {
        System.err.println(prefix + ThrowableUtils.toString(t))
    }

    fun toString(t: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        t.printStackTrace(pw)
        return sw.toString()
    }
}
