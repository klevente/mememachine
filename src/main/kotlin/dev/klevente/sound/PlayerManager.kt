package dev.klevente.sound

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer

val playerManager = DefaultAudioPlayerManager().apply {
    configuration.setFrameBufferFactory(::NonAllocatingAudioFrameBuffer)
    AudioSourceManagers.registerRemoteSources(this)
    AudioSourceManagers.registerLocalSource(this)
}