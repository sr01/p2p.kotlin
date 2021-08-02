package com.sr01.p2p.utils

interface IPAddressProvider {
    fun getConnectedWiFiIPAddress(): String
    fun getAllIPAddresses(): List<String>
}
