package com.example.ai

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val outputFile: File, val startTimeMillis: Long) : RecordingState()
    data class Finished(val audioFile: File, val durationMillis: Long) : RecordingState()
    data class Error(val message: String) : RecordingState()
}

class AudioRecorderManager(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var recordingStartTime: Long = 0L

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    fun startRecording(): Result<File> {
        stopRecordingInternal(false)

        try {
            val file = File(context.cacheDir, "audio_record_${System.currentTimeMillis()}.m4a")
            currentFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.Recording(file, recordingStartTime)
            return Result.success(file)
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error(e.message ?: "Failed to start recording")
            return Result.failure(e)
        }
    }

    fun stopRecording(): File? {
        return stopRecordingInternal(true)
    }

    private fun stopRecordingInternal(saveState: Boolean): File? {
        val file = currentFile
        try {
            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: Exception) {
                    // Ignore if recording was too brief
                }
                release()
            }
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaRecorder = null
        }

        if (saveState && file != null && file.exists() && file.length() > 0) {
            val duration = System.currentTimeMillis() - recordingStartTime
            _recordingState.value = RecordingState.Finished(file, duration)
            return file
        } else if (saveState) {
            _recordingState.value = RecordingState.Idle
        }
        return null
    }

    fun reset() {
        stopRecordingInternal(false)
        _recordingState.value = RecordingState.Idle
    }
}
