package com.chrislentner.coach.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack

class Metronome {
    private val sampleRate = 44100
    private val durationMs = 50 // Longer click for better audibility
    private val numSamples = sampleRate * durationMs / 1000
    private val generatedSnd = ByteArray(2 * numSamples)
    private var audioTrack: AudioTrack? = null

    init {
        generateClickSound()
        createAudioTrack()
    }

    private fun generateClickSound() {
        // Generate a short burst of noise with decay for a "click"
        for (i in 0 until numSamples) {
            // White noise
            var sample = ((Math.random() - 0.5) * 65535).toInt().toShort()

            // Apply linear decay envelope
            val envelope = 1.0 - (i.toDouble() / numSamples)
            sample = (sample * envelope).toInt().toShort()

            generatedSnd[2 * i] = (sample.toInt() and 0x00ff).toByte()
            generatedSnd[2 * i + 1] = ((sample.toInt() and 0xff00) ushr 8).toByte()
        }
    }

    private fun createAudioTrack() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(generatedSnd.size)
            .build()

        audioTrack?.write(generatedSnd, 0, generatedSnd.size)
    }

    fun playClick() {
        try {
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.reloadStaticData()
                track.play()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try {
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
