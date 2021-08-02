package com.sr01.p2p.peer

interface Peer<TMessage> {
    fun start()

    fun stop()

    fun connect(host: String, port: Int, onConnected: (PeerConnection<TMessage>) -> Unit)

    fun disconnect(connectionId: String)

    fun disconnectAll()
}
