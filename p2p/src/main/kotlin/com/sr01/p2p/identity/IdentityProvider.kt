package com.sr01.p2p.identity

interface IdentityProvider<out T : Identity> {
    fun get(): T
}
