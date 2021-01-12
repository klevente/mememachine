package dev.klevente.sound

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent
import com.sedmelluq.discord.lavaplayer.player.event.TrackExceptionEvent
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.VoiceChannel
import discord4j.voice.VoiceConnection
import dev.klevente.util.logger
import kotlinx.coroutines.reactive.awaitSingleOrNull
import java.util.concurrent.ConcurrentHashMap

class GuildAudioManager {

    companion object {
        private val managers: MutableMap<Snowflake, GuildAudioManager> = ConcurrentHashMap()

        fun of(id: Snowflake): GuildAudioManager = managers.getOrPut(id, ::GuildAudioManager)
        fun ofMessage(message: Message) = of(message.guildId.get())

        private val logger = logger<GuildAudioManager>()
    }

    private val currentlyPlaying
    get() = player.playingTrack != null

    private var connection: VoiceConnection? = null

    private val player: AudioPlayer = playerManager.createPlayer()
    private val provider = LavaPlayerAudioProvider(player)

    fun connectToVoiceChannel(voiceChannel: VoiceChannel) {
        connection = voiceChannel.join { it.setProvider(provider) }.block()
    }

    suspend fun leaveVoiceChannel() {
        connection ?: return
        // .awaitSingle() throws NoSuchElementException
        connection!!.disconnect().awaitSingleOrNull()
        logger.info("Left voice channel!")
        cleanup()
    }

    fun play(track: AudioTrack) {
        if (currentlyPlaying) {
            logger.info("Sound is currently playing, returning!")
            return
        }
        player.startTrack(track, false)
        logger.info("{}", player.playingTrack)
        player.addListener { event ->
            when (event) {
                is TrackEndEvent -> {
                    leaveVoiceChannelNonBlocking()
                }
                is TrackExceptionEvent -> {
                    logger.info("TrackException")
                    logger.info(event.toString())
                }
            }
        }
    }

    private fun leaveVoiceChannelNonBlocking() {
        logger.info("Leaving voice after sound end if connection is set!")
        connection ?: return
        logger.info("Connection is set and sound has finished, leaving voice!")
        val connectionToClose = connection!!
        cleanup()
        connectionToClose.disconnect().subscribe()
    }

    private fun cleanup() {
        logger.info("Cleaning up current sound context!")
        if (currentlyPlaying) {
            logger.info("Stopping track for cleanup!")
            player.stopTrack()
        }
        connection = null
    }
}