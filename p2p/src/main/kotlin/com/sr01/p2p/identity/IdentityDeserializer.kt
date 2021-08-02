package com.sr01.p2p.identity

interface IdentityDeserializer<out T : Identity> {
    fun deserialize(data: String): T
}
