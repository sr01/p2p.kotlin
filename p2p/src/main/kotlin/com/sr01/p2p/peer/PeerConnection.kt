package com.sr01.p2p.peer

interface PeerConnection<TMessage> {
    val id: String

    fun connect()

    fun disconnect()

    fun send(message: TMessage)

    fun sendAndDisconnect(message: TMessage)

    fun onConnected(function: (connection: PeerConnection<TMessage>) -> Unit)

    fun onMessage(function: (connection: PeerConnection<TMessage>, message: TMessage) -> Unit)

    fun onDisconnected(function: (connection: PeerConnection<TMessage>) -> Unit)

    fun onFailedToConnect(function: (connection: PeerConnection<TMessage>) -> Unit)
}
