package com.sr01.p2p.peer

import java.io.DataInput
import java.io.DataOutput

interface MessageProtocol<TMessage> {

    fun read(input: DataInput): TMessage

    fun write(output: DataOutput, message: TMessage)
}