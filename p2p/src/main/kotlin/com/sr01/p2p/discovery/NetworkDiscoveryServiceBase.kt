package com.sr01.p2p.discovery


import com.sr01.p2p.identity.Identity
import com.sr01.p2p.identity.IdentityDeserializer
import com.sr01.p2p.identity.IdentityProvider
import com.sr01.p2p.identity.IdentitySerializer
import com.sr01.p2p.utils.Logger
import com.sr01.p2p.utils.get
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Provides discovery service.
 * Use this to discover others, and to be discoverable by others.
 * A Facade for NetworkDiscoveryServer and INetworkDiscoveryClient.
 */
abstract class NetworkDiscoveryServiceBase<T : Identity, M : TransportMessage>(
    private val identityProvider: IdentityProvider<T>,
    private val deserializer: IdentityDeserializer<T>,
    private val serializer: IdentitySerializer<T>,
    private val logger: Logger) : NetworkDiscoveryService<T> {

    private val isDiscoverableAtomic = AtomicBoolean(false)
    private val isDiscoveringAtomic = AtomicBoolean(false)
    private var readThread: Thread? = null
    private var discoverTimer: Timer? = null
    private var discoveryListener: com.sr01.p2p.discovery.DiscoveryListener<T>? = null

    private val listenerNotifyExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val workExecutor = Executors.newSingleThreadScheduledExecutor()

    var isDiscoverable: Boolean
        get() = isDiscoverableAtomic.get()
        private set(value) {
            isDiscoverableAtomic.set(value)
        }

    var isDiscovering: Boolean
        get() = isDiscoveringAtomic.get()
        private set(value) {
            isDiscoveringAtomic.set(value)
        }

    override fun setDiscoveryListener(listener: com.sr01.p2p.discovery.DiscoveryListener<T>) {
        listenerNotifyExecutor.execute {
            discoveryListener = listener
        }
    }

    override fun clearDiscoveryListener() {
        listenerNotifyExecutor.execute {
            discoveryListener = null
        }
    }

    /**
     * Start being discoverable by others.
     */
    @Synchronized
    override fun startDiscoverable() {
        if (isDiscoverable) return

        workExecutor.execute {
            isDiscoverable = true
            logger.i(TAG, "discoverable start")
            startReadThread()
        }
    }

    /**
     * Stop being discoverable by others.
     */
    @Synchronized
    override fun stopDiscoverable() {
        if (!isDiscoverable) return

        workExecutor.execute {
            isDiscoverable = false
            workExecutor.submit {
                sendLeaveMessage()
                stopReadThread()
            }
            logger.i(TAG, "discoverable stop")
        }
    }

    /**
     * Begin discovery of others peers.
     */
    @Synchronized
    override fun startDiscover() {
        if (isDiscovering) return

        workExecutor.submit {
            isDiscovering = true
            logger.i(TAG, "discover start")
            discoverTimer = Timer()
            discoverTimer?.schedule(object : TimerTask() {
                override fun run() {
                    if (isDiscovering) {
                        sendDiscoveryRequest()
                    }
                }
            }, 0L, DISCOVER_INTERVAL_MILLISEC)
            startReadThread()
        }
    }

    /**
     * Stop discovery of other peers.
     */
    @Synchronized
    override fun stopDiscover() {
        if (!isDiscovering) return
        workExecutor.submit {
            isDiscovering = false

            discoverTimer?.cancel()

            stopReadThread()

            logger.i(TAG, "discover stop")
        }
    }

    @Synchronized
    private fun startReadThread(retryNo: Int = 0) {
        logger.d(TAG, "startReadThread, retryNo: $retryNo")

        if (readThread == null) {

            try {
                startTransport()

                readThread = Thread(this::readLoop0, TAG).apply {
                    start()
                }

                logger.d(TAG, "startReadThread, start successfully")

            } catch (e: Exception) {
                if (retryNo < MAX_START_READ_THREAD_RETRIES) {
                    logger.e(TAG, "startReadThread, failed, schedule retry", e)
                    workExecutor.schedule({
                        startReadThread(retryNo + 1)
                    }, START_READ_THREAD_RETRY_INTERVAL_SECONDS, TimeUnit.SECONDS)
                }else{
                    logger.e(TAG, "startReadThread, failed, no more retries", e)
                }
            }
        }
    }

    @Synchronized
    private fun stopReadThread() {
        if (!isDiscovering && !isDiscoverable) {
            readThread?.interrupt()
            readThread = null
            stopTransport()
        }
    }

    private fun readLoop0() {

        try {
            while (!Thread.currentThread().isInterrupted) {
                val transportMessage = readMessageFromTransport()
                transportMessage?.let {
                    handleIncomingMessage(it.data, it)
                }
            }

        } catch (e: Exception) {
            logger.d(TAG, "Error inside discoverable read thread (socket probably closed): $e")
        } finally {
            logger.d(TAG, "discoverable read thread ended")
        }
    }

    private fun handleIncomingMessage(data: String, transportMessage: M) {

        val ndsMessage = IncomingNDSMessage.parse(data)

        when (ndsMessage.type) {
            NDSMessageTypes.DiscoveryRequest -> onDiscoveryRequest(ndsMessage, transportMessage)
            NDSMessageTypes.DiscoveryResponse -> onDiscoveryResponse(ndsMessage)
            NDSMessageTypes.LeaveMessage -> onLeaveMessage(ndsMessage)
            NDSMessageTypes.Unknown -> {

            }
        }
    }

    private fun onDiscoveryRequest(message: IncomingNDSMessage, transportMessage: M) {
        if (isDiscoverable) {
            logger.d(TAG, "onDiscoveryRequest received: $message")

            val identity = deserializer.deserialize(message.data)
            if (identity == identityProvider.get()) return //ignore self identity

            sendDiscoveryResponse(transportMessage)
        }
    }

    private fun onDiscoveryResponse(message: IncomingNDSMessage) {
        if (isDiscovering) {
            logger.d(TAG, "onDiscoveryResponse received: $message")

            val identity = deserializer.deserialize(message.data)
            logger.d(TAG, "server discovered, identity: $identity")

            listenerNotifyExecutor?.execute {
                try {
                    discoveryListener?.onServerDiscovered(identity)
                } catch (e: Exception) {
                    logger.e(TAG, "failed to forward server discovery to discoveryListener: ", e)
                }
            }
        }
    }

    private fun onLeaveMessage(message: IncomingNDSMessage) {
        if (isDiscovering) {
            logger.d(TAG, "onLeaveMessage received: $message")

            val identity = deserializer.deserialize(message.data)
            if (identity == identityProvider.get()) return //ignore self identity

            logger.d(TAG, "server leaved, identity: $identity")
            discoveryListener?.onServerLeaved(identity)
        }
    }

    private fun sendLeaveMessage() {
        try {
            logger.d(TAG, "sendLeaveMessage")

            val serializedIdentity = serializer.serialize(identityProvider.get())
            val message = com.sr01.p2p.discovery.NDSParser.createLeaveMessage(serializedIdentity)
            broadcastMessage(message)

        } catch (e: Exception) {
            logger.e(TAG, "Error", e)
        }
    }

    private fun sendDiscoveryRequest() {
        try {
            logger.d(TAG, "sendDiscoveryRequest")

            val serializedIdentity = serializer.serialize(identityProvider.get())
            val message = com.sr01.p2p.discovery.NDSParser.createDiscoveryRequest(serializedIdentity)
            broadcastMessage(message)

        } catch (e: Exception) {
            logger.e(TAG, "Error", e)
        }
    }


    private fun sendDiscoveryResponse(transportMessage: M) {
        workExecutor.submit {
            try {
                logger.d(TAG, "sendDiscoveryResponse")

                val serializedIdentity = serializer.serialize(identityProvider.get())
                val message = com.sr01.p2p.discovery.NDSParser.createDiscoveryResponse(serializedIdentity)
                sendMessage(message, transportMessage)

            } catch (e: Exception) {
                logger.e(TAG, "failed to send discovery response", e)
            }
        }
    }

    protected abstract fun sendMessage(message: String, transportMessage: M)

    protected abstract fun broadcastMessage(message: String)

    protected abstract fun readMessageFromTransport(): M?

    protected abstract fun startTransport()

    protected abstract fun stopTransport()

    companion object {
        private const val DISCOVER_INTERVAL_MILLISEC = 5000L
        private const val TAG = "NDS.Base"
        private const val MAX_START_READ_THREAD_RETRIES = 10
        private const val START_READ_THREAD_RETRY_INTERVAL_SECONDS = 2L
    }
}

enum class NDSMessageTypes { Unknown, DiscoveryRequest, DiscoveryResponse, LeaveMessage }

data class IncomingNDSMessage(val type: NDSMessageTypes, val data: String) {

    companion object {
        private const val DISCOVERY_IS_THERE_ANYBODY_MESSAGE_HEADER = "is_there_anybody_out_there?"
        private const val DISCOVERY_I_AM_HERE_MESSAGE_HEADER = "im_here:"
        private const val DISCOVERY_LEAVE_MESSAGE_HEADER = "im_leaving:"

        fun parse(message: String): IncomingNDSMessage = when {
            message.startsWith(DISCOVERY_IS_THERE_ANYBODY_MESSAGE_HEADER) -> IncomingNDSMessage(NDSMessageTypes.DiscoveryRequest,
                message.substring(DISCOVERY_IS_THERE_ANYBODY_MESSAGE_HEADER.length, message.length))
            message.startsWith(DISCOVERY_I_AM_HERE_MESSAGE_HEADER) -> IncomingNDSMessage(NDSMessageTypes.DiscoveryResponse,
                message.substring(DISCOVERY_I_AM_HERE_MESSAGE_HEADER.length))
            message.startsWith(DISCOVERY_LEAVE_MESSAGE_HEADER) -> IncomingNDSMessage(NDSMessageTypes.LeaveMessage,
                message.substring(DISCOVERY_LEAVE_MESSAGE_HEADER.length, message.length))
            else -> IncomingNDSMessage(NDSMessageTypes.Unknown, message)
        }
    }
}

interface TransportMessage {
    val data: String
}