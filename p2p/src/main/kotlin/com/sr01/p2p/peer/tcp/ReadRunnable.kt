package com.sr01.p2p.peer.tcp

import com.sr01.p2p.Config
import com.sr01.p2p.peer.MessageProtocol
import com.sr01.p2p.utils.Logger
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import java.net.SocketException

class ReadRunnable<TMessage>(
    private val id: String,
    private val inputStream: InputStream,
    private val protocol: MessageProtocol<TMessage>,
    private val logger: Logger,
    private val onDataReceived: (message: TMessage) -> Unit,
    private val onDisconnect: () -> Unit

) : Runnable {

    val tag = com.sr01.p2p.Config.TAG_PEER

    override fun run() {

        Thread.currentThread().name = "Client $id Read Thread"
        var disconnect = false
        var disconnectReason: Exception? = null

        while (!disconnect && !Thread.currentThread().isInterrupted) {
            var message: TMessage? = null
            val dataStream = DataInputStream(inputStream)

            try {
                message = MessageWriter.read(dataStream, protocol)
            } catch (ex: EOFException) {
                logger.v(tag, "EOFException", ex)
                disconnect = true
                disconnectReason = ex
            } catch (ex: SocketException) {
                logger.v(tag, "SocketException", ex)
                disconnect = true
                disconnectReason = ex
            } catch (e: Exception) {
                logger.e(tag, "peer/$id, read data failed", e)
                disconnect = true
                disconnectReason = e
            }

            if (message != null) {
                try {
                    notifyDataReceived(message)
                } catch (e: Exception) {
                    logger.e(tag, "Exception", e)
                    disconnect = true
                    disconnectReason = e
                }
            }

            if (disconnect) {
                logger.v(tag, "disconnect detected, reason: $disconnectReason")
                notifyDisconnect()
            }
        }
    }

    private fun notifyDataReceived(message: TMessage) {
        onDataReceived(message)
    }

    private fun notifyDisconnect() {
        try {
            onDisconnect()
        } catch (ignored: Exception) {
        }
    }
}
