package dev.klevente.commands

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Member
import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.VoiceStateUpdateEvent
import dev.klevente.sound.GuildAudioManager
import dev.klevente.sound.playerManager
import dev.klevente.util.logger
import dev.klevente.util.removePrefix
import dev.klevente.util.replaceIndex
import dev.klevente.util.extension.getAuthorVoiceChannel
import dev.klevente.util.extension.isDisconnectedUserIdSameAs
import dev.klevente.util.extension.postReply
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.awaitSingleOrNull
import java.io.File
import kotlin.random.Random

class CommandManager(
    private val selfId: Snowflake,
    private val path: String,
) {

    private val logger = logger<CommandManager>()

    private val files = sortedSetOf<String>()
    private val random = Random(System.currentTimeMillis())

    private val commands = hashMapOf(
        "list" to ::listCommand,
        "help" to ::listCommand,
        "random" to ::randomCommand,
        "join" to ::joinCommand,
        "leave" to ::leaveCommand
    )

    suspend fun handleBotMessage(message: Message) {
        if (!message.content.startsWith(PREFIX)) {
            return
        }

        val tokenizedMessage = message
            .content
            .split(' ')
            .replaceIndex(0) {
                it.removePrefix()
            }

        val command = tokenizedMessage.first()
        commands.execute(command, message)
    }

    suspend fun handleVoiceUpdate(event: VoiceStateUpdateEvent) {
        if (event.isDisconnectedUserIdSameAs(selfId)) {
            logger.info("Bot has been disconnected, cleaning up manually!")
            GuildAudioManager.of(event.current.guildId).leaveVoiceChannel()
        }
    }

    private suspend fun listCommand(message: Message) {
        refreshFiles()
        files.fold(mutableListOf(StringBuilder()), { acc, filename ->
            if (acc.last().length + filename.length > MAX_MESSAGE_LENGTH) {
                acc.add(StringBuilder())
            }
            acc.last().appendLine(filename)
            acc
        }).forEach {
            message.postReply(it.toString())
        }
    }

    private suspend fun soundCommand(message: Message) {
        playSound(message, message.content.removePrefix())
    }

    private suspend fun randomCommand(message: Message) {
        if (files.isEmpty()) {
            refreshFiles()
        }
        val selected = files.random(random)
        playSound(message, selected)
    }

    private suspend fun joinCommand(message: Message) {
        GuildAudioManager
            .ofMessage(message)
            .connectToVoiceChannel(message.getAuthorVoiceChannel())
    }

    private suspend fun leaveCommand(message: Message) {
        GuildAudioManager
            .ofMessage(message)
            .leaveVoiceChannel()
    }

    private suspend fun isUserOnVoice(member: Member): Boolean {
        return member.voiceState.awaitSingleOrNull() != null
    }

    private fun fileExists(sound: String): Boolean {
        if (files.contains(sound)) {
            return true
        }
        refreshFiles()
        if (files.contains(sound)) {
            return true
        }
        return false
    }

    private fun refreshFiles() {
        val soundDirectory = File(path)
        val filesOnDisk = soundDirectory
            .list { _, name -> name.endsWith(".mp3", ignoreCase = true) }!!
            .map { it.substringBefore('.') }
        files.addAll(filesOnDisk)
    }

    private suspend fun playSound(message: Message, sound: String) {
        logger.info("Play sound: $sound")
        if (!isUserOnVoice(message.authorAsMember.awaitSingle())) {
            logger.info("User not on voice, aborting!")
            message.postReply("You are not in a voice channel!")
            return
        }
        if (!fileExists(sound)) {
            logger.info("File named $sound does not exist, aborting!")
            message.postReply("There is no sound named $sound!")
            return
        }
        val audioManager = GuildAudioManager.ofMessage(message)

        val voiceChannel = message.getAuthorVoiceChannel()

        playerManager.loadItemOrdered(audioManager, "$path/$sound.mp3", object : AudioLoadResultHandler {
            override fun trackLoaded(audioTrack: AudioTrack) {
                audioManager.connectToVoiceChannel(voiceChannel)
                audioManager.play(audioTrack)
            }

            override fun playlistLoaded(audioPlaylist: AudioPlaylist) = Unit

            override fun noMatches() = Unit

            override fun loadFailed(exception: FriendlyException) {
                logger.error("Error when loading sound file: ", exception)
            }
        })
    }

    private suspend fun Map<String, suspend (Message) -> Unit>.execute(command: String, message: Message) {
        val lowercaseCommand = command.toLowerCase()
        if (!containsKey(lowercaseCommand)) {
            logger.info("Sound command called with $command!")
            soundCommand(message)
        } else {
            logger.info("${command.capitalize()} has been called!")
            getValue(lowercaseCommand).invoke(message)
        }
    }
}