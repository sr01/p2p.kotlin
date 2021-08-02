package com.sr01.p2p.discovery

import com.sr01.p2p.identity.Identity
import com.sr01.p2p.identity.IdentityDeserializer
import com.sr01.p2p.identity.IdentityProvider
import com.sr01.p2p.utils.Logger
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.charset.Charset

abstract class NDSReadThread<T : Identity>(var identityProvider: IdentityProvider<T>,
                                           val deserializer: IdentityDeserializer<T>,
                                           val tag: String,
                                           val logger: Logger) : Runnable {

    private fun readSocket(socket: DatagramSocket, receiveBuffer: ByteArray) {
        try {
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
            socket.receive(receivePacket)
            val message = String(receivePacket.data, 0, receivePacket.length, Charset.forName("utf-8"))
            logger.v(tag, "received: \r\n\tdata: $message\r\n\tfrom address: ${receivePacket.address.hostAddress}")

            val discoveryRequest = com.sr01.p2p.discovery.NDSParser.parseDiscoveryRequest(message)
            if (discoveryRequest != null) {
                val requestIdentity = deserializer.deserialize(discoveryRequest)
                if (requestIdentity == identityProvider.get()) return //ignore self identity
                onDiscoveryRequest(receivePacket.address, receivePacket.port, socket)
            } else {
                val discoveryResponse = com.sr01.p2p.discovery.NDSParser.parseDiscoveryResponse(message)
                if (discoveryResponse != null) {
                    val identity = deserializer.deserialize(discoveryResponse)
                    onDiscoveryResponse(identity, receivePacket.address)
                } else {
                    val leaveMessage = com.sr01.p2p.discovery.NDSParser.parseLeaveMessage(message)
                    if (leaveMessage != null) {
                        val requestIdentity = deserializer.deserialize(leaveMessage)
                        if (requestIdentity == this.identityProvider.get()) return //ignore self identity
                        onLeaveMessage(requestIdentity)
                    }
                }
            }

        } catch (ignored: SocketTimeoutException) {
        }
    }

    override fun run() {
        logger.d(tag, "discoverable read thread started")

        val receiveData = ByteArray(15000)

        try {
            while (!Thread.currentThread().isInterrupted) {
                val socket = socket
                readSocket(socket, receiveData)
            }

        } catch (e: Exception) {
            logger.d(tag, "Error inside discoverable thread (socket probably closed): $e")
        } finally {
            logger.d(tag, "discoverable read thread ended")
        }
    }

    fun stop() {
        closeSocket()
    }

    @Synchronized
    private fun closeSocket() {
        try {
            socket.close()
        } catch (ignored: Exception) {
        }
    }

    abstract val socket: DatagramSocket

    abstract fun onDiscoveryRequest(address: InetAddress, port: Int, socket: DatagramSocket)

    abstract fun onDiscoveryResponse(identity: T, address: InetAddress)

    abstract fun onLeaveMessage(identity: T)
}

