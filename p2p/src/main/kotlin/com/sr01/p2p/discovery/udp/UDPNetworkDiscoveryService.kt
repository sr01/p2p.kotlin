package com.sr01.p2p.discovery.udp


import com.sr01.p2p.discovery.DiscoveryMessage
import com.sr01.p2p.discovery.NetworkDiscoveryServiceBase
import com.sr01.p2p.identity.Identity
import com.sr01.p2p.identity.IdentityDeserializer
import com.sr01.p2p.identity.IdentityProvider
import com.sr01.p2p.identity.IdentitySerializer
import com.sr01.p2p.utils.IPAddressProvider
import com.sr01.p2p.utils.Logger
import com.sr01.p2p.utils.broadcastTo
import com.sr01.p2p.utils.send
import java.net.*
import java.nio.charset.Charset

@Suppress("unused")
class UDPNetworkDiscoveryService<T : Identity>(identityProvider: IdentityProvider<T>,
                                               deserializer: IdentityDeserializer<T>,
                                               serializer: IdentitySerializer<T>,
                                               private val ipAddressProvider: IPAddressProvider,
                                               private val logger: Logger,
                                               private val port: Int = 8888) : NetworkDiscoveryServiceBase<T, com.sr01.p2p.discovery.DiscoveryMessage>(identityProvider, deserializer, serializer, logger) {

    private var socket: DatagramSocket? = null
    private var isShutdown = false

    override fun readMessageFromTransport(): com.sr01.p2p.discovery.DiscoveryMessage? {
        socket?.let { socket ->
            try {
                logger.d(TAG, "readMessageFromTransport, socket: $socket")
                val receiveBuffer = ByteArray(15000)
                val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket.receive(receivePacket)
                val message = String(receivePacket.data, 0, receivePacket.length, Charset.forName("utf-8"))
                logger.d(TAG, "received: \r\n\tdata: $message\r\n\tfrom address: ${receivePacket.address.hostAddress}")
                return com.sr01.p2p.discovery.DiscoveryMessage(message, receivePacket.address, receivePacket.port)

            } catch (ignored: SocketTimeoutException) {
            }catch (sx: SocketException) {
                if(!isShutdown){
                    sx.printStackTrace()
                }
            }
        }
        return null
    }

    @Synchronized
    override fun startTransport() {
        isShutdown = false
        socket = DatagramSocket(port, InetAddress.getByName("0.0.0.0")).apply {
            broadcast = true
            soTimeout = 10000
        }
    }

    @Synchronized
    override fun stopTransport() {
        try {
            isShutdown = true
            socket?.close()
            socket = null
        } catch (ignored: Exception) {
        }
    }

    override fun sendMessage(message: String, transportMessage: com.sr01.p2p.discovery.DiscoveryMessage) {
        socket?.let { socket ->
            if (!socket.isClosed) {
                val data = message.toByteArray(charset("utf-8"))
                socket.send(data, transportMessage.address, transportMessage.port)
            }
        }
    }

    override fun broadcastMessage(message: String) {
        socket?.let { socket ->
            if (!socket.isClosed) {
                val addresses = ipAddressProvider.getAllIPAddresses()
                addresses.forEach { address ->
                    logger.d(TAG, "send broadcast message to $address on port $port")
                    val data = message.toByteArray(charset("utf-8"))
                    socket.broadcastTo(data, address, port)
                }
            }
        }
    }

    companion object {
        private const val TAG = "NDS.UDP"
    }
}
