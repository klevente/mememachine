# mememachine

**mememachine** is a simple Discord bot that plays custom-supplied sounds in a voice chat.

## Installation

Getting **mememachine** up and running is very easy:
1. Install Java on the machine where you want the bot to reside
2. Download the precompiled `.jar` or build it from source using `mvn package` (in this case, use the `with-dependencies` version)
3. Create a bot account with permissions: Send Messages, Connect, Speak
4. Acquire token from website
5. Download some `.mp3`s and place them in a folder
6. Run the bot using: `java -jar mememachine.jar <your-token> <path-to-sounds-folder>`

## Usage

Once the bot is online, the following commands are available (the bot's prefix is `%`):
* `list`/`help`: List out available sounds to play
* `random`: Play a random sound
* `join`: Join a voice chat (used for testing)
* `leave`: Leave the voice chat (used for testing)
* `<soundname>`: Play the sound named `<soundname>` (without the `.mp3` extension)

Every command except `list/help` only works when the requesting user is in a voice channel, as the bot needs to know 
where to join.

The bot can also be disconnected using Discord's Disconnect function in the context menu. It will also disconnect upon 
being moved to another channel.

## Management

Adding new sounds is easy: just copy them to the supplied folder, and the bot will pick them up automatically, as long as they are in `.mp3`. The name will be derived from the sound's filename, without the `.mp3` extension.

For changing the prefix, `dev.klevente.commands.Config.kt` can be edited. Don't forget to `mvn package` after.