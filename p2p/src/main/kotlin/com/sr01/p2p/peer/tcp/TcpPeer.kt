package com.sr01.p2p.peer.tcp


import com.sr01.p2p.Config
import com.sr01.p2p.peer.*
import com.sr01.p2p.utils.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

@Suppress("unused")
open class TcpPeer<TMessage>(val localPort: Int, private val protocol: MessageProtocol<TMessage>, val logger: Logger) : Peer<TMessage> {

    private val tag: String = com.sr01.p2p.Config.TAG_PEER
    private val connectionMap: MutableMap<String, PeerConnection<TMessage>>
    private val server: TcpPeerServer<TMessage>
    private val executor = Executors.newSingleThreadExecutor()
    private val connectionFactory: TcpPeerConnectionFactory<TMessage> = TcpPeerConnectionFactory(protocol, logger)
    private var onIncomingConnection: (connection: PeerConnection<TMessage>) -> Unit = {}
    private var isStarted = false

    init {
        connectionMap = ConcurrentHashMap()
        server = TcpPeerServer(connectionFactory, localPort, logger)
        server.onIncomingConnection = { connection ->
            executor.execute {
                this@TcpPeer.logger.i(tag, "peer/$localPort, new connection: ${connection.id}")
                addConnection(connection.id, connection)
                onIncomingConnection(connection)
            }
        }
    }

    override fun start() {
        executor.execute {
            if (!isStarted) {
                server.start()
            }
        }
    }

    override fun stop() {
        executor.execute {
            if (isStarted) {
                server.stop()
                internalDisconnectAll()
            }
        }
    }

    override fun connect(host: String, port: Int, onConnectionCreated: (PeerConnection<TMessage>) -> Unit) {
        executor.execute {
            val connectionId = createConnectionId(host, port)
            val peerConnection = connectionMap[connectionId]
            when {
                peerConnection != null -> {
                    logger.d(tag, "peer/$localPort, request to connect ignored: already connected to $connectionId")
                    onConnectionCreated(peerConnection)
                }
                else -> {
                    val peerConnection = connectionFactory.create(ConnectionInfo(connectionId, host, port))
                    addConnection(connectionId, peerConnection)
                    peerConnection.connect()
                    onConnectionCreated(peerConnection)
                }
            }
        }
    }

    override fun disconnect(connectionId: String) {
        executor.execute {
            val connection = connectionMap[connectionId]
            if (connection == null) {
                logger.d(tag, "peer/$localPort, request to disconnect ignored: no connection found with id: $connectionId")
            } else {
                connectionMap.remove(connectionId)
                connection.disconnect()
            }
        }
    }

    override fun disconnectAll() {
        executor.execute {
            internalDisconnectAll()
        }
    }

    fun onIncomingConnection(onIncomingConnection: (connection: PeerConnection<TMessage>) -> Unit) {
        executor.execute {
            this.onIncomingConnection = onIncomingConnection
        }
    }

    private fun internalDisconnectAll() {
        val connections = ArrayList(connectionMap.values)
        for (con in connections) {
            con.disconnect()
        }
    }

    private fun addConnection(connectionId: String, connection: PeerConnection<TMessage>) {
        connectionMap.put(connectionId, connection)
        connection.onConnected {
            this@TcpPeer.logger.i(tag, "peer/$localPort, connection connected: $connectionId")
        }
        connection.onDisconnected {
            this@TcpPeer.logger.i(tag, "peer/$localPort, connection disconnected: $connectionId")
            connectionMap.remove(connectionId)
        }
        connection.onFailedToConnect {
            connectionMap.remove(connectionId)
        }
    }

    private fun createConnectionId(host: String, port: Int): String = String.format("%s:%d", host, port)
}
