package com.sr01.p2p.peer.tcp


import com.sr01.p2p.Config
import com.sr01.p2p.peer.PeerConnection
import com.sr01.p2p.utils.Logger
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.Executors

class TcpPeerServer<TMessage>(private val connectionFactory: TcpPeerConnectionFactory<TMessage>, val port: Int = 7000, private val logger: Logger) {

    private val tag = com.sr01.p2p.Config.TAG_SERVER
    private var serverThread: Thread? = null
    private var serverSocket: ServerSocket? = null
    private val executor = Executors.newSingleThreadExecutor()

    var isActive = false
        private set

    var onStart: () -> Unit = {}
    var onStop: () -> Unit = {}
    var onIncomingConnection: (connection: PeerConnection<TMessage>) -> Unit = {}

    fun start() {
        executor.execute {
            if (!isActive) {
                logger.d(tag, "server/${port}, starting")
                isActive = true
                val serverThread = Thread(ServerThread(), "TcpPeerServer-AcceptThread")
                serverThread.start()
                this.serverThread = serverThread
            }
        }
    }

    fun stop() {
        executor.execute {
            if (isActive) {
                logger.d(tag, "server/${port}, stopping")
                isActive = false
                val serverSocket = this.serverSocket
                if (serverSocket != null) {
                    try {
                        serverSocket.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }

                this.serverThread?.interrupt()
            }
        }
    }

    private inner class ServerThread : Runnable {

        override fun run() {

            try {
                serverSocket = ServerSocket(port)
                logger.i(tag, "server/${port}, started")

                onStart()

                while (isActive && !Thread.currentThread().isInterrupted) {

                    try {
                        val socket = serverSocket?.accept()
                        if (socket != null) {
                            val connection = connectionFactory.create(socket)
                            onIncomingConnection(connection)
                        }

                    } catch (ignored: IOException) {
                    }

                }

            } catch (e: IOException) {
                logger.w(tag, "server/${port}, exception in server thread", e)
            }

            onStop()

            logger.i(tag, "server/${port}, stopped")
        }
    }
}
