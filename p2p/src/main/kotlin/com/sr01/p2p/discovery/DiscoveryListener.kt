package com.sr01.p2p.discovery


import com.sr01.p2p.identity.Identity

interface DiscoveryListener<in T : Identity> {
    fun onServerDiscovered(identity: T)
    fun onServerLeaved(identity: T)
}
