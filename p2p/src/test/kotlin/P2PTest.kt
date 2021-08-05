import com.sr01.p2p.peer.MessageProtocol
import com.sr01.p2p.peer.tcp.TcpPeer
import com.sr01.p2p.utils.ConsoleLogger
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.DataInput
import java.io.DataOutput
import kotlin.test.assertEquals

/**
 * Created by Shmulik on 19/01/2018.
 * .
 */
class P2PTest {

    interface TestMessage
    data class TextMessage(val id: Int, val content: String) : TestMessage
    data class AckMessage(val id: Int, val ackedMessageId: Int) : TestMessage
    enum class MessageType { Text, Ack }

    object TestMessageProtocol : MessageProtocol<TestMessage> {
        override fun read(input: DataInput): TestMessage =
            when (MessageType.values()[input.readInt()]) {
                MessageType.Text -> TextMessage(input.readInt(), input.readUTF())
                MessageType.Ack -> AckMessage(input.readInt(), input.readInt())
            }

        override fun write(output: DataOutput, message: TestMessage) {
            when (message) {
                is TextMessage -> {
                    output.writeInt(MessageType.Text.ordinal)
                    output.writeInt(message.id)
                    output.writeUTF(message.content)
                }
                is AckMessage -> {
                    output.writeInt(MessageType.Ack.ordinal)
                    output.writeInt(message.id)
                    output.writeInt(message.ackedMessageId)
                }
            }
        }
    }

    private lateinit var peer1: TcpPeer<TestMessage>
    private lateinit var peer2: TcpPeer<TestMessage>

    @Before
    fun setUp() {

        val logger = ConsoleLogger
        peer1 = TcpPeer(8001, TestMessageProtocol, logger)
        peer2 = TcpPeer(8002, TestMessageProtocol, logger)

        peer1.start()
        peer2.start()
    }

    @After
    fun tearDown() {
        peer1.stop()
        peer2.stop()
    }

    @Test
    fun connectAndSend() {

        val messageHolder = arrayOf<TestMessage?>(null)
        val expectedMessage = TextMessage(7, "Hello from 1")

        peer2.connect("localhost", peer1.localPort) { connection ->
            connection.onConnected {
                it.send(expectedMessage)
            }
        }

        peer1.onIncomingConnection { connection ->
            connection.onMessage { _, message ->
                messageHolder[0] = message
            }
        }

        val actualMessage = getUntilNotNull(1000, 5000) {
            messageHolder[0]
        }

        assertEquals(expectedMessage, actualMessage)
    }
}