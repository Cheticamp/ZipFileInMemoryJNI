package com.example.zipfileinmemoryjni

// From https://gist.github.com/ncruces/0e6e625899645f5a88e4d7444d696130 and converted to Kotlin
// with some minor modifications.

import java.nio.ByteBuffer
import java.nio.channels.SeekableByteChannel

class ByteBufferChannel(buffer: ByteBuffer) : SeekableByteChannel {
    private val buf: ByteBuffer = buffer

    @Synchronized
    override fun read(dst: ByteBuffer): Int {
        if (buf.remaining() == 0) return -1
        val count = dst.remaining().coerceAtMost(buf.remaining())
        if (count > 0) {
            buf.slice().apply {
                limit(count)
                dst.put(this)
            }
            buf.position(buf.position() + count)
        }
        return count
    }

    @Synchronized
    override fun position(newPosition: Long): ByteBufferChannel {
        require(newPosition or Int.MAX_VALUE - newPosition >= 0)
        buf.position(newPosition.toInt())
        return this
    }

    override fun write(src: ByteBuffer?): Int {
        TODO("Not yet implemented")
    }

    @Synchronized
    override fun size() = buf.limit().toLong()

    @Synchronized
    override fun position() = buf.position().toLong()

    override fun isOpen() = true

    override fun close() {}

    override fun truncate(size: Long): SeekableByteChannel {
        TODO("Not yet implemented")
    }
}