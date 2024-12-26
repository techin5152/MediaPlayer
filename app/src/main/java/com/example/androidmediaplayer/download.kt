package com.example.androidmediaplayer

import android.content.Context
import android.os.Handler
import android.util.Log
import android.widget.Toast
import okhttp3.*
import java.io.File
import java.io.IOException

fun downloadFileFromDrive(
    context: Context,
    url: String,
    fileName: String,
    onComplete: (File?) -> Unit
) {
    val tempFile = File(context.cacheDir, fileName)

    val request = Request.Builder()
        .url(url)
        .build()

    OkHttpClient().newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e("Download", "Failed to download file: $url", e)
            Handler(context.mainLooper).post {
                Toast.makeText(context, "Failed to download file.", Toast.LENGTH_SHORT).show()
            }
            onComplete(null)
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                response.body?.let { body ->
                    tempFile.outputStream().use { outputStream ->
                        body.byteStream().copyTo(outputStream)
                    }
                    Handler(context.mainLooper).post {
                        Toast.makeText(context, "File downloaded: $fileName", Toast.LENGTH_SHORT).show()
                    }
                    onComplete(tempFile)
                } ?: run {
                    Handler(context.mainLooper).post {
                        Toast.makeText(context, "Empty response body.", Toast.LENGTH_SHORT).show()
                    }
                    onComplete(null)
                }
            } else {
                Handler(context.mainLooper).post {
                    Toast.makeText(context, "Failed response: ${response.code}", Toast.LENGTH_SHORT).show()
                }
                onComplete(null)
            }
        }
    })
}
