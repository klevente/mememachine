package dev.klevente.util.extension

import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.VoiceChannel
import kotlinx.coroutines.reactive.awaitSingle

suspend fun Message.getAuthorVoiceChannel(): VoiceChannel =
    authorAsMember.awaitSingle().voiceState.awaitSingle().channel.awaitSingle()

suspend fun Message.postReply(content: String) {
    channel.awaitSingle().createMessage(content).awaitSingle()
}