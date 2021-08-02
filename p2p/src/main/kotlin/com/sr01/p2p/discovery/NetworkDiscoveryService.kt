package com.sr01.p2p.discovery

import com.sr01.p2p.identity.Identity

interface NetworkDiscoveryService<T : Identity> {

    fun setDiscoveryListener(listener: com.sr01.p2p.discovery.DiscoveryListener<T>)

    fun clearDiscoveryListener()

    fun startDiscoverable()

    fun stopDiscoverable()

    fun startDiscover()

    fun stopDiscover()
}