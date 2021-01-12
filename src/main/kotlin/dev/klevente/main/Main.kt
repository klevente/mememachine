package dev.klevente.main

import discord4j.core.DiscordClient
import discord4j.core.event.domain.VoiceStateUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import dev.klevente.commands.CommandManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.system.exitProcess

private fun printUsage() {
    println("Usage: java -jar mememachine.jar token path_to_sounds")
}

fun main(args: Array<String>) {
    when (args.size) {
        0 -> {
            println("Missing required params!")
            printUsage()
            return
        }
        1 -> {
            println("Missing token or sound directory!")
            printUsage()
            return
        }
    }

    val token = args[0]
    val path = args[1]

    if (!Files.isDirectory(Paths.get(path))) {
        println("Supplied path is not valid!")
        return
    }

    val client = DiscordClient.create(token)
    val manager = CommandManager(
        selfId = client.coreResources.selfId,
        path = path
    )

    client.withGateway { gateway ->
        val message = mono {
            gateway.on(MessageCreateEvent::class.java)
                .asFlow()
                .collect {
                    manager.handleBotMessage(it.message)
                }
        }
        val voiceUpdate = mono {
            gateway.on(VoiceStateUpdateEvent::class.java)
                .asFlow()
                .collect {
                    manager.handleVoiceUpdate(it)
                }
        }
        return@withGateway Mono.`when`(message, voiceUpdate)
    }.block()

    exitProcess(0)
}