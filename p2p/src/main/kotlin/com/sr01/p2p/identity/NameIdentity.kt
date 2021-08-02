package com.sr01.p2p.identity

data class NameIdentity(override val name: String, override val description: String, val host: String) : Identity
