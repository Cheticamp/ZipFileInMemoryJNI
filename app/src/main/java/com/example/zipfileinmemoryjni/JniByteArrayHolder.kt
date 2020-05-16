package com.example.zipfileinmemoryjni

import java.nio.ByteBuffer

class JniByteArrayHolder {
    external fun allocate(size: Long): ByteBuffer?
    external fun freeBuffer(byteBuffer: ByteBuffer)

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}