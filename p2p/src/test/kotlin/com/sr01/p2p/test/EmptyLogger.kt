package com.sr01.p2p.test

object EmptyLogger : com.sr01.p2p.utils.Logger {
    override fun e(tag: String, msg: String, e: Throwable?) {
    }

    override fun w(tag: String, msg: String, e: Throwable?) {
    }

    override fun i(tag: String, msg: String, e: Throwable?) {
    }

    override fun d(tag: String, msg: String, e: Throwable?) {
    }

    override fun v(tag: String, msg: String, e: Throwable?) {
    }

}