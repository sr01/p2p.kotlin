package com.sr01.p2p.utils

fun <T> T?.get(doIfNotNull: T.() -> Unit): OrElseUnit {
    if (this != null) doIfNotNull(this)
    return OrElseUnit()
}

class OrElseUnit {
    fun orElse(function: () -> Unit) {
        function()
    }
}
