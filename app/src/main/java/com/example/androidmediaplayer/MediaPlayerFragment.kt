package com.example.androidmediaplayer

import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

class MediaPlayerFragment : Fragment() {

    private lateinit var videoView: VideoView
    private lateinit var imageView: ImageView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resumeButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var statusIcon: ImageView
    private lateinit var durationTextView: TextView
    private lateinit var countdownTextView: TextView

    private val mediaList = mutableListOf<MediaItem>()
    private var currentIndex = 0
    private var isPlaying = false
    private var isPaused = false
    private val handler = Handler(Looper.getMainLooper())
    private var elapsedTime = 0

    private val video = "VDO"
    private val image = "IMAGE"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_media_player, container, false)

        videoView = view.findViewById(R.id.videoView)
        imageView = view.findViewById(R.id.imageView)
        startButton = view.findViewById(R.id.startButton)
        stopButton = view.findViewById(R.id.stopButton)
        pauseButton = view.findViewById(R.id.pauseButton)
        resumeButton = view.findViewById(R.id.resumeButton)
        progressBar = view.findViewById(R.id.progressBar)
        statusIcon = view.findViewById(R.id.statusIcon)
        durationTextView = view.findViewById(R.id.durationTextView)
        countdownTextView = view.findViewById(R.id.countdownTextView)

        // Set VideoView to full screen
        val layoutParams = videoView.layoutParams
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        videoView.layoutParams = layoutParams

        loadMediaList()
        setupClickListeners()

        return view
    }

    private fun setupClickListeners() {
        startButton.setOnClickListener {
            if (!isPlaying) {
                isPlaying = true
                isPaused = false
                toggleButtonsVisibility(false)
                updateButtonColors("start")
                playMedia()
                updateIcon("playing")
            }
        }

        stopButton.setOnClickListener {
            stopMedia()
            updateButtonColors("stop")
            updateIcon("stopped")
        }

        pauseButton.setOnClickListener {
            if (isPlaying && !isPaused) {
                isPaused = true
                if (videoView.isPlaying) {
                    videoView.pause()
                } else {
                    handler.removeCallbacksAndMessages(null)
                }
            }
            updateButtonColors("pause")
            updateIcon("paused")
        }

        resumeButton.setOnClickListener {
            if (isPlaying && isPaused) {
                isPaused = false
                if (videoView.visibility == VideoView.VISIBLE && !videoView.isPlaying) {
                    // กลับสู่การเล่นวิดีโออีกครั้ง
                    val remainingTime = elapsedTime
                    if (!videoView.isPlaying) {
                        videoView.start()
                    }
                    startCountdown(remainingTime)
                } else if (imageView.visibility == ImageView.VISIBLE) {
                    // กลับมาเริ่มนับถอยหลังเพื่อเล่นภาพอีกครั้ง
                    val remainingTime = elapsedTime
                    if (remainingTime > 0) {
                        handler.postDelayed({
                            playNextMedia()
                        }, remainingTime * 1000L)
                    } else {
                        playNextMedia()
                    }

                    startCountdown(remainingTime)
                }
                updateButtonColors("resume")
                updateIcon("playing")
            }
        }

        //ดัก error popup vdo
        videoView.setOnErrorListener { _, _, _ ->
            Toast.makeText(
                requireActivity(),
                "เกิดข้อผิดพลาดในการแสดง video: ${mediaList[currentIndex].filePath}",
                Toast.LENGTH_SHORT
            ).show()
            playNextMedia()
            true
        }

        // เมื่อวิดีโอพร้อมจะเล่น (หลังจากโหลดเสร็จ)
        videoView.setOnPreparedListener {
            progressBar.visibility = ProgressBar.GONE
            videoView.start()
        }

        // หลังจากวิดีโอเล่นจบ
        videoView.setOnCompletionListener {
            playNextMedia()
        }

        // เมือมีกดแตะที่หน้าจอ Vdo
        videoView.setOnTouchListener { _, _ ->
            toggleButtonsVisibility(true)

            handler.postDelayed({
                toggleButtonsVisibility(false)
            }, 2000)
            true
        }

        // เมือมีกดแตะที่หน้าจอ image
        imageView.setOnTouchListener { _, _ ->
            toggleButtonsVisibility(true)

            handler.postDelayed({
                toggleButtonsVisibility(false)
            }, 2000)
            true
        }
    }

    private fun loadMediaList() {
        try {
            val inputStream = requireContext().assets.open("media_config.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            reader.close()

            val gson = Gson()
            val listType = object : TypeToken<List<MediaItem>>() {}.type
            val mediaItems: List<MediaItem> = gson.fromJson(jsonString, listType)

            mediaList.clear()
            mediaList.addAll(mediaItems)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error loading media list", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playMedia() {
        if (currentIndex >= mediaList.size) {
            currentIndex = 0
        }

        val currentMedia = mediaList[currentIndex]
        updateDurationText(currentMedia.duration)

        if (currentMedia.filePath.endsWith(".mp4")) {
            // กรณี Video
            val filePath = currentMedia.filePath.substringBeforeLast(".")
            imageView.visibility = ImageView.GONE
            videoView.visibility = VideoView.VISIBLE
            progressBar.visibility = ProgressBar.VISIBLE

            try {
                videoView.setVideoURI(Uri.parse("android.resource://${requireContext().packageName}/raw/${filePath}")) //ไฟล์
                videoView.setOnPreparedListener {
                    progressBar.visibility = ProgressBar.GONE // ซ่อนแถบความคืบหน้าเมื่อวิดีโอพร้อม
                    videoView.start()
                    startCountdown(currentMedia.duration, video)
                    handler.postDelayed({
                        if (videoView.isPlaying) {
                            videoView.stopPlayback() // หยุดวิดีโอเมื่อถึงเวลาที่กำหนด
                            playNextMedia()
                        }
                    }, currentMedia.duration * 1000L)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireActivity(),
                    "เกิดข้อผิดพลาดในการแสดง video: ${currentMedia.filePath}",
                    Toast.LENGTH_SHORT
                ).show()
                playNextMedia()
            }
        } else {
            // กรณี Image
            videoView.visibility = VideoView.GONE
            imageView.visibility = ImageView.VISIBLE
            progressBar.visibility = ProgressBar.GONE

            try {
                val inputStream = requireContext().assets.open(currentMedia.filePath)
                val drawable = Drawable.createFromStream(inputStream, null)
                imageView.setImageDrawable(drawable)
                startCountdown(currentMedia.duration)

                handler.postDelayed({
                    playNextMedia()
                }, currentMedia.duration * 1000L)
            } catch (e: Exception) {
                Toast.makeText(
                    requireActivity(),
                    "เกิดข้อผิดพลาดในการแสดงรูปภาพ: ${currentMedia.filePath}",
                    Toast.LENGTH_SHORT
                ).show()
                playNextMedia()
            }
        }
    }

    private fun playNextMedia() {
        if (isPlaying) {
            videoView.stopPlayback() // ล้างการเล่นก่อนหน้า
            currentIndex++
            playMedia()
        }
    }

    private fun stopMedia() {
        isPlaying = false
        videoView.stopPlayback()
        handler.removeCallbacksAndMessages(null)
        currentIndex = 0
        progressBar.visibility = ProgressBar.GONE
        imageView.visibility = ImageView.GONE
        durationTextView.visibility = TextView.GONE
        countdownTextView.visibility = TextView.GONE
    }

    private fun toggleButtonsVisibility(visible: Boolean) {
        val visibility = if (visible) Button.VISIBLE else Button.GONE
        startButton.visibility = visibility
        stopButton.visibility = visibility
        pauseButton.visibility = visibility
        resumeButton.visibility = visibility
    }

    private fun updateButtonColors(mode: String) {
        val defaultColor = ContextCompat.getColor(requireActivity(), R.color.default_button)
        val activeColor = ContextCompat.getColor(requireActivity(), R.color.active_button)

        startButton.setBackgroundColor(if (mode == "start") activeColor else defaultColor)
        stopButton.setBackgroundColor(if (mode == "stop") activeColor else defaultColor)
        pauseButton.setBackgroundColor(if (mode == "pause") activeColor else defaultColor)
        resumeButton.setBackgroundColor(if (mode == "resume") activeColor else defaultColor)
    }

    private fun updateIcon(status: String) {
        when (status) {
            "playing" -> statusIcon.setImageResource(R.drawable.ic_playing)
            "paused" -> statusIcon.setImageResource(R.drawable.ic_paused)
            "stopped" -> statusIcon.setImageResource(R.drawable.ic_stopped)
        }
        statusIcon.visibility = ImageView.VISIBLE
        handler.postDelayed({
            statusIcon.visibility = ImageView.GONE
        }, 1000) // Show for 1 second
    }

    private fun updateDurationText(duration: Int) {
        durationTextView.text = "ระยะเวลา: $duration"
        durationTextView.visibility = TextView.VISIBLE
    }

    private fun startCountdown(duration: Int, type: String = image) {
        countdownTextView.visibility = TextView.VISIBLE
        Log.w("startCountdown", "$type")

        if (type == video) {
            var remainingTime = duration
            handler.post(object : Runnable {
                override fun run() {
                    if (remainingTime > 0 && isPlaying && !isPaused && videoView.isPlaying) {
                        countdownTextView.text = "เวลาที่เหลืออยู่: $remainingTime"
                        remainingTime--
                        elapsedTime = remainingTime
                        handler.postDelayed(this, 1000)
                    }
                }
            })
        } else {
            var remainingTime = duration
            handler.post(object : Runnable {
                override fun run() {
                    if (remainingTime > 0 && isPlaying && !isPaused) {
                        countdownTextView.text = "เวลาที่เหลืออยู่: $remainingTime"
                        remainingTime--
                        elapsedTime = remainingTime
                        handler.postDelayed(this, 1000)
                    }
                }
            })
        }
    }
}