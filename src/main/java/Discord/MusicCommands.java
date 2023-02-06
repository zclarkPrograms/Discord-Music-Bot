package Discord;

import Playlist.AudioPackage;
import Playlist.Playlist;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Future;

/**
 * Purpose of this class is to help with managing commands across different servers.
 * Each server needs its own Playlist, AudioManager, AudioPlayer, AudioStream.
 * This class will provide static methods that allow us to manage these separate instances.
 */
public class MusicCommands {
    private static HashMap<String, AudioPackage> playingServers = new HashMap<>();

    // This will contain Guild IDs of servers who need to have their "now playing" messages killed.
    // Primarily for when a user uses the /nowplaying command. This serves as a "kill signal" to the previous now playing message.
    private static ArrayList<String> killMessageUpdater = new ArrayList<>();

    private static final int timeout = 10; // How long a bot should be idle before disconnecting (in seconds).

    /**
     * Add a new song to the queue, or create a new queue and play the song if one doesn't exist.
     * Returns null if a new playlist had to be made.
     * Returns the song name if it added the song to an existing queue.
     * @param event Command event interaction.
     * @param playtop
     * @return
     */
    public static AudioPackage play(SlashCommandInteractionEvent event, AudioChannel channel, String searchKey, boolean playtop) {
        String guid = event.getGuild().getId();
        AudioPackage returnVal;
        boolean isNew = false;
        if(!searchKey.endsWith(".mp3")) {
            if (!searchKey.startsWith("http")) {
                searchKey = "ytsearch: " + searchKey;
            }
        }


        System.out.println("Searching for " + searchKey);

        // If the server already has an active music bot.
        // Use the existing AudioPackage to queue a new song.
        // This assumes there already exists a Playlist.
        if (playingServers.containsKey(guid)) {
            System.out.println("Guild playlist exists. Adding to queue.");
            AudioPackage pkg = playingServers.get(guid);
            pkg.staleTimer = 0;
            Future f = pkg.stream.loadItem(searchKey, pkg.handler); // Search for the item.
            while(!f.isDone()) {
                //System.out.println("Waiting for f 2");
                // It would be preferred to implement a lock or semaphore to avoid busy waiting in the future.
                // For now this should be fine since it doesn't take long to search.
            }
            //System.out.println("Storing requestee: " + pkg.handler.track.getInfo().title + "  " + event.getUser().getName());
            pkg.playlist.requestedBy.put(pkg.handler.track, event.getUser().getName());
            if (pkg.player.getPlayingTrack() == null) { // If there is no currently playing track, start playing instantly.
                pkg.player.playTrack(pkg.handler.track);
                importUserPlaylist(event, pkg);
            }
            else {
                if (playtop) {
                    pkg.playlist.playtop(pkg.handler.track); // Only add it next in queue if there's already a song playing.
                }
                else {
                    pkg.playlist.play(pkg.handler.track);
                }
                importUserPlaylist(event, pkg);
            }
            returnVal = pkg;
            isNew = false;
        }

        // If the server does NOT have an active music bot.
        else {
            System.out.println("Guild playlist does not exist. Creating queue.");
            AudioPackage pkg = new AudioPackage();
            pkg.staleTimer = 0;
            AudioStream.audioPlayer(searchKey, channel, pkg);
            Playlist playlist = new Playlist(pkg.handler.track);
            pkg.playlist = playlist;
            pkg.serverID = event.getGuild().getId();
            if(pkg.handler.playlist!=null)
                System.out.println("Attempting to play track: " + pkg.handler.playlist.getSelectedTrack());
            pkg.player.playTrack(pkg.handler.track);
            playingServers.put(guid, pkg);
            pkg.scheduler.pkg = pkg;
            //System.out.println("Storing requestee: " + pkg.handler.track.getInfo().title + "  " + event.getUser().getName());
            pkg.playlist.requestedBy.put(pkg.handler.track, event.getUser().getName());

            importUserPlaylist(event, pkg);
            returnVal = pkg;
            isNew = true;
        }

        // Moved from Discord.java to here.
        AudioPackage playlistExists = returnVal;
        if (playlistExists.handler.playlist.isSearchResult()) {
            event.getHook().sendMessageEmbeds(
                    EmbedReplies.player_addedTrackToQueue(event.getUser().getName(),
                            playlistExists.handler.track.getSourceManager().getSourceName(),
                            playlistExists.handler.track.getInfo().title).build()).queue();
        }
        else {
            event.getHook().sendMessageEmbeds(
                    EmbedReplies.player_AddedPlaylistToQueue(event.getUser().getName(),
                            playlistExists.handler.track.getSourceManager().getSourceName(),
                            playlistExists.handler.playlist).build()).queue();

        }
        if (isNew) {
            Discord.generateNowPlaying(event);
        }

        return returnVal;
    }

    private static void importUserPlaylist(GenericInteractionCreateEvent event, AudioPackage pkg) {
        if (pkg.handler.playlist!=null && !pkg.handler.playlist.isSearchResult()) {
            for (AudioTrack track : pkg.handler.playlist.getTracks()) {
                if (track != pkg.handler.playlist.getTracks().get(0)) {
                    pkg.playlist.play(track); // Only add it next in queue if there's already a song playing.
                    System.out.println("Adding song from playlist: " + track.getInfo().title + "  " + event.getUser().getName());
                    pkg.playlist.requestedBy.put(track, event.getUser().getName());
                }
            }
        }
    }

    public static ArrayList<EmbedBuilder> getQueuePages(GenericInteractionCreateEvent event) {
        String guid = event.getGuild().getId();
        if (playingServers.containsKey(guid)) {
            return playingServers.get(guid).queuePages;
        }
        return null;
    }

    public static EmbedBuilder getNextQueuePage(GenericInteractionCreateEvent event) {
        String guid = event.getGuild().getId();
        String interId = guid;
        System.out.println("interId: " + interId);
        if (playingServers.containsKey(guid)) {
            AudioPackage pkg = playingServers.get(guid);
            if (!pkg.queueMessagePageTracker.containsKey(interId)) {
                System.out.println("New Page Tracker! Getting index 1");
                pkg.queueMessagePageTracker.put(interId, 2); // The next page to grab will be 1
                return pkg.queuePages.get(1); // Get the first page from the queue pages.
            }
            else {
                int page = pkg.queueMessagePageTracker.get(interId);
                if (page >= getQueuePages(event).size()) {
                    return pkg.queuePages.get(page - 1);
                }
                pkg.queueMessagePageTracker.replace(interId, page + 1);
                System.out.println("Page Tracker retrieving page " + page);
                return pkg.queuePages.get(page);
            }
        }
        return null;
    }

    public static boolean isOverflowedQueue(GenericInteractionCreateEvent event) {
        String guid = event.getGuild().getId();
        if (playingServers.containsKey(guid)) {
            System.out.println("Queue Page Size :: " + playingServers.get(guid).queuePages.size());
            if(playingServers.get(guid).queuePages.size() >= 1) {
                return true;
            }
        }
        return false;
    }

    public static void setQueuePages(GenericInteractionCreateEvent event, ArrayList<EmbedBuilder> arr) {
        String guid = event.getGuild().getId();
        if (playingServers.containsKey(guid)) {
            playingServers.get(guid).queuePages = arr;
        }
    }

    /**
     * Pause the playing track.
     * @param event Pass Guild event (used for getting guild ID)
     */
    public static void pause(GenericInteractionCreateEvent event) {
        String guid = event.getGuild().getId();
        if (playingServers.containsKey(guid)) {
            playingServers.get(guid).player.setPaused(true);
        }
    }

    /**
     * Resume a track if paused.
     * @param event Pass Guild event (used for getting guild ID)
     */
    public static void resume(GenericInteractionCreateEvent event) {
        String guid = event.getGuild().getId();
        if (playingServers.containsKey(guid)) {
            playingServers.get(guid).player.setPaused(false);
        }
    }

    /**
     * Skip the currently playing track.
     * @param event Pass Guild event (used for getting guild ID)
     * @return -1 if there is no song currently playing, 1 if successful.
     */
    public static int skip(GenericInteractionCreateEvent event) {
        String guid = event.getGuild().getId();
        if (playingServers.containsKey(guid)) {
            if(playingServers.get(guid).player.getPlayingTrack()==null){
                return -1;
            }

            AudioTrack nextTrack = playingServers.get(guid).playlist.next();
            System.out.println(nextTrack);

            playingServers.get(guid).player.stopTrack();
            playingServers.get(guid).player.playTrack(nextTrack);

            /*if (nextTrack == null) {
                //playingServers.get(guid).player.stopTrack(); Don't stop the current track
                return 1;
            }*/
        }
        return 1;
    }

    /**
     * Skip to a specified song in the queue.
     * @param event Pass Guild event (used for getting guild ID)
     * @param index index of the song to skip to
     * @return -1 if there is no song currently playing, 1 if successful.
     */
    public static int skipto(GenericInteractionCreateEvent event, int index) {
        String guid = event.getGuild().getId();
        if(playingServers.containsKey(guid)) {
            if(playingServers.get(guid).player.getPlayingTrack()==null){
                return -1;
            }

            AudioTrack nextTrack = playingServers.get(guid).playlist.skipto(index);
            System.out.println(nextTrack);

            playingServers.get(guid).player.stopTrack();
            playingServers.get(guid).player.playTrack(nextTrack);
        }
        return 1;
    }

    public static void clear(GenericInteractionCreateEvent event) {
        String guid = event.getGuild().getId();

        playingServers.get(guid).playlist.clear();
    }

    /**
     * Get the playlist queue for a specific guild.
     * @param event Pass Guild event (used for getting guild ID)
     * @return ArrayList of AudioTracks as queue.
     */
    public static ArrayList<AudioTrack> getQueue(GenericInteractionCreateEvent event) {
        String guid = event.getGuild().getId();
        if (playingServers.containsKey(guid)) {
            return playingServers.get(guid).playlist.queue();
        }
        return null;
    }


    /**
     * Get the username of the person requesting a given track.
     * @param track Track of whose requester to grab.
     * @param guildId Guild ID
     * @return Username of the person that requested the given track.
     */
    public static String getTrackRequester(AudioTrack track, String guildId) {
        if (MusicCommands.getPlaylist(guildId).requestedBy.containsKey(track)) {
            return MusicCommands.getPlaylist(guildId).requestedBy.get(track);
        }
        return null;
    }


    /**
     * Get the player for a specific guild.
     * @param guid Server ID of the Guild.
     * @return AudioPlayer for the given guild.
     */
    public static AudioPlayer getPlayer(String guid) {
        if (playingServers.containsKey(guid)) {
            return playingServers.get(guid).player;
        }
        System.out.println("No player found");
        return null;
    }

    public static AudioManager getManager(String guid) {
        if (playingServers.containsKey(guid)) {
            return playingServers.get(guid).manager;
        }
        System.out.println("No player found");
        return null;
    }

    /**
     * Get the playlist for a specific guild.
     * @param guid Server ID of the Guild.
     * @return Playlist for the given guild.
     */
    public static Playlist getPlaylist(String guid) {
        if (playingServers.containsKey(guid)) {
            return playingServers.get(guid).playlist;
        }
        return null;
    }

    /**
     * End a guild session. Destroys the player and kills the audio connection to DC from voice channel.
     * @param guid Guild ID
     */
    public static void shutdown(String guid) {
        System.out.println("Shutting down bot for guild " + guid);
        if (playingServers.containsKey(guid)) {
            AudioPackage pkg = playingServers.get(guid);
            pkg.player.destroy();
            pkg.manager.closeAudioConnection();
            playingServers.remove(guid);
        }
    }

    /**
     * Used to increment the stale timer for a session. See MusicCommands.isTimedOut() for usage.
     * @param guid Guild ID
     */
    public static void incrementTimeout(String guid) {
        if (playingServers.containsKey(guid)) {
            AudioPackage pkg = playingServers.get(guid);
            pkg.staleTimer++;
        }
    }

    /**
     * A bot is considered to be "timed out" if it is connected to a voice channel but is not being used to play a track.
     * The bot will also be considered inactive if there are no other people in the voice channel.
     * To change the timeout time, modify the "timeout" variable in this class.
     * @param guid Guild ID
     * @return True if the bot has timed out, False if the bot is not timed out.
     */
    public static boolean isTimedOut(String guid) {
        if (playingServers.containsKey(guid)) {
            AudioPackage pkg = playingServers.get(guid);
            if (pkg.staleTimer >= timeout) {
                return true;
            }
        }
        return false;
    }


    /**
     * Kill any existing "Now Playing" messages for a given session or guild.
     * This method simply adds the guild ID to a list of "to kill" sessions.
     * In the Now Playing message handler in Discord.java, it will stop message updates if the Guild ID is in this list.
     * We do this for rate limit purposes. If too many messages are being updated, the bot will be limited by Discord.
     * @param guid Guild ID
     */
    public static void killExistingMessages(String guid) {
        killMessageUpdater.add(guid);
    }

    /**
     * Checks if the message has been replaced. Can be run dry in that it won't remove the GUID after checking.
     * If not dry run, it will remove the GUID from the to-kill list. Used in the Discord.java message updater to check if the updater needs to be stopped.
     * @param guid Guild ID
     * @param dryRun True to only check if the message needs to be replaced. False to consider the message replaced and remove it from the to-kill list.
     * @return True if the message has been replaced by a newer one. False if otherwise.
     */
    public static boolean messageReplaced(String guid, boolean dryRun) {
        if (killMessageUpdater.contains(guid)) {
            if (!dryRun) {
                killMessageUpdater.remove(guid);
            }
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * A session is considered "active" if it is connected to a voice channel. This does not mean the session is in progress.
     * There is a separate "timeout" in which a bot may be connected to a voice channel, but has not been used.
     * @param guid Guild ID
     * @return True if the session is active. False if otherwise.
     */
    // A session is considered "active" if it is connected to a voice channel. This does not mean the session is in progress.
    // There is a separate "timeout" in which a bot may be connected to a voice channel, but has not been used.
    public static boolean isActive(String guid) {
        if (playingServers.containsKey(guid)) {
            return true;
        }
        return false;
    }



     public static boolean loop(String guid) {
        if (playingServers.containsKey(guid)) {
            return playingServers.get(guid).playlist.loop();
        }
        System.err.println("Couldn't find Guild in active servers!");
        return false;
     }
}
