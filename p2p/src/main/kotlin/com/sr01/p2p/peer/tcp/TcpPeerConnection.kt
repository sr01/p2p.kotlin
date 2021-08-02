package com.sr01.p2p.peer.tcp


import com.sr01.p2p.Config
import com.sr01.p2p.peer.*
import com.sr01.p2p.utils.Logger
import com.sr01.p2p.utils.ThrowableUtils
import java.io.DataOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TcpPeerConnection<TMessage>(override val id: String, private val host: String, private val port: Int, private val protocol: MessageProtocol<TMessage>, val logger: Logger) :
    PeerConnection<TMessage> {

    private val tag = com.sr01.p2p.Config.TAG_CONNECTION + "-$id"
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var state = STATE_DISCONNECTED
    private var readThread: Thread? = null
    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private val listenersObservable = PeerConnectionObservable()
    private val incomingMessagesQueue = LinkedList<TMessage>()
    private var onMessage: ((PeerConnection<TMessage>, TMessage) -> Unit)? = null

    constructor(id: String, socket: Socket, protocol: MessageProtocol<TMessage>, logger: Logger)
            : this(id, socket.remoteSocketAddress.toString(), socket.port, protocol, logger) {

        executor.execute {
            initSocketAndStartReadThread(socket)
            state = STATE_CONNECTED
            logger.d(tag, "state change to connected")
            notify(EVENT_CONNECTED)
        }
    }

    override fun connect() {
        executor.execute {
            if (state == STATE_DISCONNECTED) {
                state = STATE_CONNECTING
                logger.d(tag, "state change to connecting")
                internalConnect()
            }
        }
    }

    override fun disconnect() {
        if (state == STATE_CONNECTED || state == STATE_CONNECTING) {
            executor.execute {
                if (state == STATE_CONNECTED || state == STATE_CONNECTING) {
                    state = STATE_DISCONNECTING

                    logger.d(tag, "state change to disconnecting")

                    internalDisconnect()

                    executor.shutdownNow()
                }
            }
        }
    }

    override fun send(message: TMessage) {
        executor.execute {
            logger.d(tag, "send message: $message")
            try {
                val stream = outputStream
                if (stream != null) {
                    MessageWriter.write(stream, protocol, message)
                } else {
                    //send(message)
                    logger.e(tag, "can't send, output stream is null!")
                }
            } catch (e: Exception) {
                System.err.println(Thread.currentThread().name + " Exception: " + ThrowableUtils.toString(e))
            }
        }
    }

    override fun sendAndDisconnect(message: TMessage) {
        executor.execute {
            this.send(message)
            this.disconnect()
        }
    }

    override fun toString(): String {
        return "TcpPeerConnection{id='$id}"
    }

    override fun onConnected(function: (connection: PeerConnection<TMessage>) -> Unit) {
        executor.execute {
            addObserver(EVENT_CONNECTED, function)
        }
    }

    override fun onDisconnected(function: (connection: PeerConnection<TMessage>) -> Unit) {
        executor.execute {
            logger.d(tag, "onDisconnected, add observer function: $function")
            addObserver(EVENT_DISCONNECTED, function)
        }
    }

    override fun onFailedToConnect(function: (connection: PeerConnection<TMessage>) -> Unit) {
        executor.execute {
            addObserver(EVENT_FAILED_TO_CONNECT, function)
        }
    }

    override fun onMessage(function: (connection: PeerConnection<TMessage>, message: TMessage) -> Unit) {
        executor.execute {

            while (!incomingMessagesQueue.isEmpty()) {
                logger.d(tag, "dequeue message from preProcessedMessagesQueue")
                val message = incomingMessagesQueue.remove()
                function(this, message)
            }

            logger.d(tag, "set onMessage function callback")
            onMessage = function
        }
    }


    @Throws(IOException::class)
    private fun initSocketAndStartReadThread(socket: Socket) {
//        socket.soTimeout = 2000

        this.socket = socket

        this.outputStream = DataOutputStream(socket.getOutputStream())

        val runnable = ReadRunnable(id, socket.getInputStream(), protocol, logger,
            onDataReceived = { message ->
                logger.d(tag, "message received: $message")
                onIncomingMessage(message)
            },
            onDisconnect = {
                disconnect()
            })

        val readThread = Thread(runnable, "TcpPeerConnection-ReadThread-$id")

        readThread.start()

        this.readThread = readThread
    }

    private fun onIncomingMessage(message: TMessage) {
        executor.execute {
            val onMessageFunc = onMessage
            if (onMessageFunc != null) {
                onMessageFunc(this, message)
            } else {
                logger.d(tag, "onIncomingMessage onMessageFunc is null, queue message")
                incomingMessagesQueue.add(message)
            }
        }
    }

    private fun internalConnect() {
        try {
            val s = Socket()
            s.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLISECONDS)
            initSocketAndStartReadThread(s)
            state = STATE_CONNECTED
            logger.d(tag, "state change to connected")
            notify(EVENT_CONNECTED)

        } catch (e: IOException) {
            logger.e(tag, "failed to connect", e)
            notify(EVENT_FAILED_TO_CONNECT)
            internalDisconnect()
        }

    }

    private fun internalDisconnect() {
        readThread?.interrupt()

        notify(EVENT_DISCONNECTED)

        try {
            socket?.close()
            socket = null
        } catch (e: IOException) {
            logger.e(tag, "failed to disconnect", e)
        } finally {
            state = STATE_DISCONNECTED
            logger.d(tag, "state change to disconnected")
        }
    }

    private fun notify(event: Int) {
        listenersObservable.notifyObservers(ConnectionEventParams(event, this))
    }

    private fun addObserver(event: Int, function: (connection: PeerConnection<TMessage>) -> Unit) {
        listenersObservable.addObserver { _, arg ->
            @Suppress("UNCHECKED_CAST")
            val params: ConnectionEventParams<TMessage> = arg as ConnectionEventParams<TMessage>
            when (params.event) {
                event -> function(params.connection)
            }
        }
    }

    private inner class PeerConnectionObservable : Observable() {
        override fun notifyObservers(data: Any) {
            setChanged()
            super.notifyObservers(data)
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MILLISECONDS = 5000
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTING = 1
        private const val STATE_CONNECTED = 2
        private const val STATE_DISCONNECTING = 3

        const val EVENT_CONNECTED = 1
        const val EVENT_FAILED_TO_CONNECT = 2
        const val EVENT_DISCONNECTED = 3
    }
}

data class ConnectionEventParams<TMessage>(val event: Int, val connection: PeerConnection<TMessage>)
