package com.sr01.p2p.identity

interface IdentitySerializer<T : Identity> {
    fun serialize(identity: T): String
}
