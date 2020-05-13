package com.example.zipfileinmemoryjni;

import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.nio.ByteBuffer;

import static java.lang.Math.min;

public final class ByteBufferChannel extends SeekableInMemoryByteChannel {
    private final ByteBuffer buf;

    public ByteBufferChannel(ByteBuffer buffer) {
        if (buffer == null) throw new NullPointerException();
        buf = buffer;
    }

    @Override
    public synchronized int read(ByteBuffer dst) {
        if (buf.remaining() == 0) return -1;

        int count = min(dst.remaining(), buf.remaining());
        if (count > 0) {
            ByteBuffer tmp = buf.slice();
            tmp.limit(count);
            dst.put(tmp);
            buf.position(buf.position() + count);
        }
        return count;
    }

    @Override
    public synchronized long position() {
        return buf.position();
    }

    @Override
    public synchronized ByteBufferChannel position(long newPosition) {
        if ((newPosition | Integer.MAX_VALUE - newPosition) < 0)
            throw new IllegalArgumentException();
        buf.position((int) newPosition);
        return this;
    }

    @Override
    public synchronized long size() {
        return buf.limit();
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() {
    }
}