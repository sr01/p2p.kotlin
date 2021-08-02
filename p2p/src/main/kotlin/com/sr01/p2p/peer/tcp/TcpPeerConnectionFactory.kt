package com.sr01.p2p.peer.tcp

import com.sr01.p2p.peer.MessageProtocol
import com.sr01.p2p.peer.PeerConnection
import com.sr01.p2p.utils.Logger
import java.net.InetSocketAddress
import java.net.Socket

class TcpPeerConnectionFactory<TMessage>(private val protocol: MessageProtocol<TMessage>, private val logger: Logger) {

    fun create(data: Any): PeerConnection<TMessage> =
        when (data) {
            is Socket -> {
                val remoteAddress = data.remoteSocketAddress as InetSocketAddress
                val host = remoteAddress.hostName
                val port = remoteAddress.port
                val id = String.format("%s:%d", host, port)

                TcpPeerConnection(id, data, this.protocol, logger)
            }
            is ConnectionInfo -> TcpPeerConnection(data.id, data.host, data.port, protocol, logger)
            else -> throw RuntimeException("can't create connection by the provided argument: $data")
        }
}
