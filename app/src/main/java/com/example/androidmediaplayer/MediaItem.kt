package com.example.androidmediaplayer

import com.google.gson.annotations.SerializedName

data class MediaItem(@SerializedName("file_path") val filePath: String, val duration: Int)
