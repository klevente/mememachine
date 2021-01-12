package dev.klevente.util.extension

import discord4j.common.util.Snowflake
import discord4j.core.event.domain.VoiceStateUpdateEvent

fun VoiceStateUpdateEvent.isDisconnectedUserIdSameAs(id: Snowflake): Boolean {
    if (!isLeaveEvent) {
        return false
    }
    if (!old.isPresent) {
        return false
    }
    val leftUserId = old.get().userId
    return id == leftUserId
}