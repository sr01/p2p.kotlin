import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/**
 * Created by shmulik on 11/01/2018.
 */

class GZIPtest {

    @Test
    fun test1(){

        val outBuffer = ByteArrayOutputStream()
        val gzip = GZIPOutputStream(outBuffer)
        val outputStream = DataOutputStream(gzip)

        val expected =  "Hello World this is GZIP stream"
        outputStream.writeUTF(expected)
        outputStream.writeBytes(expected)
        gzip.finish()

        val inBuffer = outBuffer.toByteArray()
        val inputStream = DataInputStream(GZIPInputStream(ByteArrayInputStream(inBuffer)))
        val actual = inputStream.readUTF()

        println(actual)
        Assert.assertEquals(expected, actual)

    }
}