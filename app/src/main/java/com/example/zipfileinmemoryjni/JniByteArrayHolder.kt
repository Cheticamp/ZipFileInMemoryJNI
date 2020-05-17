package com.example.zipfileinmemoryjni

import java.nio.ByteBuffer

class JniByteArrayHolder {
    external fun allocate(size: Int): ByteBuffer?
    external fun freeBuffer(byteBuffer: ByteBuffer)

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}