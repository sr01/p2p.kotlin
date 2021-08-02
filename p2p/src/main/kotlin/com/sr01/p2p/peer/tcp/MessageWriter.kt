package com.sr01.p2p.peer.tcp

import com.sr01.p2p.peer.MessageProtocol
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Created by Shmulik on 15/01/2018.
 * .
 */
object MessageWriter {

    fun <TMessage> write(outputStream: DataOutputStream, protocol: MessageProtocol<TMessage>, message: TMessage) {
        val buffer = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(buffer)
        protocol.write(DataOutputStream(gzip), message)
        gzip.finish()

        val data = buffer.toByteArray()
        outputStream.writeInt(data.size)
        outputStream.write(data)
    }

    fun <TMessage> read(inputStream: DataInputStream, protocol: MessageProtocol<TMessage>): TMessage {

        val size = inputStream.readInt()
        val data: ByteArray = readData(size, inputStream)
        return protocol.read(DataInputStream(GZIPInputStream(ByteArrayInputStream(data))))
    }

    private fun readData(size: Int, stream: InputStream): ByteArray {
        val data = ByteArray(size)
        var totalRead = 0
        while (totalRead < size) {
            val actualRead = stream.read(data, totalRead, size - totalRead)
            if (actualRead > 0) {
                totalRead += actualRead
            } else {
                throw EOFException()
            }
        }
        return data
    }
}