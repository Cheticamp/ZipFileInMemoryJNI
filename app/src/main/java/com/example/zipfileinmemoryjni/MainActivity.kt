package com.example.zipfileinmemoryjni

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onClick(view: View) {
        button.isEnabled = false
        status.text = getString(R.string.running)

        thread {
            printMemStats("Before buffer allocation:")
            var bufferSize = 0L
            // testzipfile.zip is not part of the project but any zip can be uploaded through the
            // device file manager or adb to test.
            val fileToRead = "$filesDir/testzipfile.zip"
            val inStream =
                if (File(fileToRead).exists()) {
                    FileInputStream(fileToRead).apply {
                        bufferSize = getFileSize(this)
                        close()
                    }
                    FileInputStream(fileToRead)
                } else {
                    // If testzipfile.zip doesn't exist, we will just look at this one which
                    // is part of the APK.
                    resources.openRawResource(R.raw.appapk).apply {
                        bufferSize = getFileSize(this)
                        close()
                    }
                    resources.openRawResource(R.raw.appapk)
                }
            // Allocate the buffer in native memory (off-heap).
            val jniByteArrayHolder = JniByteArrayHolder()
            val byteBuffer =
                if (bufferSize != 0L) {
                    jniByteArrayHolder.allocate(bufferSize)?.apply {
                        printMemStats("After buffer allocation")
                    }
                } else {
                    null
                }

            if (byteBuffer == null) {
                Log.d("Applog", "Failed to allocate $bufferSize bytes of native memory.")
            } else {
                Log.d("Applog", "Allocated ${Formatter.formatFileSize(this, bufferSize)} buffer.")
                val inBytes = ByteArray(4096)
                Log.d("Applog", "Starting buffered read...")
                while (inStream.available() > 0) {
                    byteBuffer.put(inBytes, 0, inStream.read(inBytes))
                }
                inStream.close()
                byteBuffer.flip()
                ZipFile(ByteBufferChannel(byteBuffer)).use {
                    Log.d("Applog", "Starting Zip file name dump...")
                    for (entry in it.entries) {
                        Log.d("Applog", "Zip name: ${entry.name}")
                        val zis = it.getInputStream(entry)
                        while (zis.available() > 0) {
                            zis.read(inBytes)
                        }
                    }
                }
                printMemStats("Before buffer release:")
                jniByteArrayHolder.freeBuffer(byteBuffer)
                printMemStats("After buffer release:")
            }
            runOnUiThread {
                status.text = getString(R.string.idle)
                button.isEnabled = true
                Log.d("Applog", "Done!")
            }
        }
    }

    /*
        This function is a little misleading since it does not reflect the true status of memory.
        After native buffer allocation, it waits until the memory is used before counting is as
        used. After release, it doesn't seem to count the memory as released until garbage
        collection. (My observations only.) Also, see the comment for memset() in native-lib.cpp
        which is a member of this project.
    */
    private fun printMemStats(desc: String? = null) {
        val memoryInfo = ActivityManager.MemoryInfo()
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
        val nativeHeapSize = memoryInfo.totalMem
        val nativeHeapFreeSize = memoryInfo.availMem
        val usedMemInBytes = nativeHeapSize - nativeHeapFreeSize
        val usedMemInPercentage = usedMemInBytes * 100 / nativeHeapSize
        val sDesc = desc?.run { "$this:\n" }
        Log.d(
            "AppLog", "$sDesc total:${Formatter.formatFileSize(this, nativeHeapSize)} " +
                    "free:${Formatter.formatFileSize(this, nativeHeapFreeSize)} " +
                    "used:${Formatter.formatFileSize(this, usedMemInBytes)} ($usedMemInPercentage%)"
        )
    }

    // Not a great way to do this but not the object of the demo.
    private fun getFileSize(inStream: InputStream): Long {
        var bufferSize = 0L
        while (inStream.available() > 0) {
            val toSkip = inStream.available().toLong()
            inStream.skip(toSkip)
            bufferSize += toSkip
        }
        return bufferSize
    }
}