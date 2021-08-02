package com.sr01.p2p.utils


import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.Charset

/**
 * Broadcast data. The broadcast network is extracted from the specified IP address.
 */
fun DatagramSocket.broadcastTo(sendData: ByteArray, ipAddress: String, port: Int) {
    val broadcastIPAddress = ipAddress.substring(0, ipAddress.lastIndexOf(".")) + ".255"
    val inetAddress = InetAddress.getByName(broadcastIPAddress)
    val sendPacket = DatagramPacket(sendData, sendData.size, inetAddress, port)
    send(sendPacket)
}

fun DatagramSocket.send(data: ByteArray, address: InetAddress, port: Int) {
    val sendPacket = DatagramPacket(data, data.size, address, port)
    send(sendPacket)
}

