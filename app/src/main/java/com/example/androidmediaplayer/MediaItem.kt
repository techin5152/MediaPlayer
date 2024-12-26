package com.example.androidmediaplayer

import com.google.gson.annotations.SerializedName

data class MediaItem(
    @SerializedName("file_path") val filePath: String,
    val duration: Int,
    val filename: String ,
    var localFilePath: String? = null,
    val isVideo: Boolean
)
