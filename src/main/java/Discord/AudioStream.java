package Discord;

import Playlist.AudioPackage;
import Playlist.AudioSender;
import Playlist.LoadResultHandler;
import Playlist.Scheduler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 * Sets up necessary components to stream audio to a Discord channel.
 */
public class AudioStream implements AudioSendHandler {
    public static Scheduler scheduler;
    public static AudioPlayer player;
    public static AudioManager manager;
    /**
     * LavaPlayer setup: creates and manages audio streams which will be used to send to Discord bot.
     *
     */
    public static AudioPlayer audioPlayer(String search, AudioChannel channel, AudioPackage callingPackage) {
        AudioPlayerManager stream = new DefaultAudioPlayerManager(); // Create a audio connection manager
        AudioSourceManagers.registerRemoteSources(stream); // Only need one audio manager for all Guilds (multithreaded)
        stream.registerSourceManager(new YoutubeAudioSourceManager(true)); // permit YouTube streams & searches
        stream.registerSourceManager(new LocalAudioSourceManager());
        player = stream.createPlayer();
        scheduler = new Scheduler(player, channel.getGuild().getId(), callingPackage);
        player.addListener(scheduler);
        LoadResultHandler h = new LoadResultHandler();
        Future f = stream.loadItem(search, h);
        while(!f.isDone()) {
            //System.out.println("Waiting for f");
        }
        callingPackage.stream = stream; // Set the AudioPlayerManager in the calling AudioPackage. Allows us to use these commands later.
        callingPackage.player = player;
        callingPackage.handler = h;
        callingPackage.scheduler = scheduler;
        openAudioChannel(channel,player); // Channel ID of which Discord voice channel to join
        callingPackage.manager = manager;
        return player; // Return the player
    }

    public static boolean destroyAudioPlayer(SlashCommandInteractionEvent event) {
        try {
            AudioPlayer player = MusicCommands.getPlayer(Objects.requireNonNull(event.getGuild()).getId());
            AudioManager manager = MusicCommands.getManager(event.getGuild().getId());
            assert manager != null;
            assert player != null;
            player.destroy();
            manager.closeAudioConnection();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Discord.exceptionToMessage = e;
            return false;
        }
    }

    /**
     * Open an audio channel on Discord.
     * ! Limited to one per guild.
     */
    public static void openAudioChannel(AudioChannel audioChannel, AudioPlayer player) {
        //Guild guild = Discord.getGuildInstance(); // only for testing purposes
        // We want this to work with all servers.
        Guild guild = audioChannel.getGuild();
        VoiceChannel channel = guild.getVoiceChannelById(audioChannel.getId());
        manager = guild.getAudioManager();
        manager.setSendingHandler(new AudioSender(player));
        manager.openAudioConnection(channel);
    }




    // From AudioSendHandler

    @Override
    public boolean canProvide() {
        return false;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        return null;
    }

    @Override
    public boolean isOpus() {
        return AudioSendHandler.super.isOpus();
    }
}
