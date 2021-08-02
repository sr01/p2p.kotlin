package com.sr01.p2p.identity

data class NameIdentity(override val name: String, override val description: String, val host: String, val port: Int) : Identity {
    companion object {
        val empty = NameIdentity("", "", "", 0)
    }
}
