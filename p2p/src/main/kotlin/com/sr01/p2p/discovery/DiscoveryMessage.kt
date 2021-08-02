package com.sr01.p2p.discovery

import java.net.InetAddress

data class DiscoveryMessage(override val data: String, val address: InetAddress, val port: Int) : com.sr01.p2p.discovery.TransportMessage