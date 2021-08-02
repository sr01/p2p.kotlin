package com.sr01.p2p.discovery

internal object NDSParser {

    private const val DISCOVERY_IS_THERE_ANYBODY_MESSAGE_HEADER = "is_there_anybody_out_there?"
    private const val DISCOVERY_I_AM_HERE_MESSAGE_HEADER = "im_here:"
    private const val DISCOVERY_LEAVE_MESSAGE_HEADER = "im_leaving:"

    fun createDiscoveryRequest(serializedIdentity: String): String =
            com.sr01.p2p.discovery.NDSParser.DISCOVERY_IS_THERE_ANYBODY_MESSAGE_HEADER + serializedIdentity

    fun createDiscoveryResponse(serializedIdentity: String): String =
            com.sr01.p2p.discovery.NDSParser.DISCOVERY_I_AM_HERE_MESSAGE_HEADER + serializedIdentity

    fun createLeaveMessage(serializedIdentity: String): String =
            com.sr01.p2p.discovery.NDSParser.DISCOVERY_LEAVE_MESSAGE_HEADER + serializedIdentity


    fun parseDiscoveryRequest(message: String): String? {
        if (message.startsWith(com.sr01.p2p.discovery.NDSParser.DISCOVERY_IS_THERE_ANYBODY_MESSAGE_HEADER)) {
            return message.substring(com.sr01.p2p.discovery.NDSParser.DISCOVERY_IS_THERE_ANYBODY_MESSAGE_HEADER.length, message.length)
        }
        return null
    }

    fun parseDiscoveryResponse(message: String): String? {
        if (message.startsWith(com.sr01.p2p.discovery.NDSParser.DISCOVERY_I_AM_HERE_MESSAGE_HEADER)) {
            return message.substring(com.sr01.p2p.discovery.NDSParser.DISCOVERY_I_AM_HERE_MESSAGE_HEADER.length)
        }
        return null
    }

    fun parseLeaveMessage(message: String): String? {
        if (message.startsWith(com.sr01.p2p.discovery.NDSParser.DISCOVERY_LEAVE_MESSAGE_HEADER)) {
            return message.substring(com.sr01.p2p.discovery.NDSParser.DISCOVERY_LEAVE_MESSAGE_HEADER.length, message.length)
        }
        return null
    }


}
