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
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private fun printMemStats() {
        val memoryInfo = ActivityManager.MemoryInfo()
        (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
        val nativeHeapSize = memoryInfo.totalMem
        val nativeHeapFreeSize = memoryInfo.availMem
        val usedMemInBytes = nativeHeapSize - nativeHeapFreeSize
        val usedMemInPercentage = usedMemInBytes * 100 / nativeHeapSize
        Log.d(
            "AppLog", "total:${Formatter.formatFileSize(this, nativeHeapSize)} " +
                    "free:${Formatter.formatFileSize(this, nativeHeapFreeSize)} " +
                    "used:${Formatter.formatFileSize(this, usedMemInBytes)} ($usedMemInPercentage%)"
        )
    }

    fun onClick(view: View) {
        button.isEnabled = false
        status.text = "Running..."
        thread {
            printMemStats()
            val jniByteArrayHolder = JniByteArrayHolder()
            val byteBuffer = jniByteArrayHolder.allocate(800 * 1024 * 1024)
            printMemStats()
            val inStream = resources.openRawResource(R.raw.appapk)
            val inBytes = ByteArray(4096)
            Log.d("Applog", "Starting buffered read...")
            while (inStream.available() > 0) {
                inStream.read(inBytes)
                byteBuffer.put(inBytes)
            }
            byteBuffer.flip()
            val zipFile = ZipFile(ByteBufferChannel(byteBuffer))
            zipFile.use {
                Log.d("Applog", "Starting Zip file name dump...")
                for (entry in zipFile.entries) {
                    val name = entry.name
                    Log.d("Applog", "Zip name: $name")
                }
            }
            jniByteArrayHolder.freeBuffer(byteBuffer)
            runOnUiThread {
                status.text = "Done!"
                button.isEnabled = true
            }
        }
    }
}