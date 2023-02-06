package Discord;

import Playlist.Playlist;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;

public class EmbedReplies {
    private static final String red_excla_url = "https://emojipedia-us.s3.dualstack.us-west-1.amazonaws.com/thumbs/160/emoji-one/44/heavy-exclamation-mark-symbol_2757.png";
    private static final String yikes_jam = "https://cdn.discordapp.com/emojis/887875965705928764.gif?size=96&quality=lossless";
    private static final String pepe_jam = "https://cdn.discordapp.com/emojis/517652117155217423.gif?size=96&quality=lossless";

    public static EmbedBuilder genericErrorBuilder(String title, Color color, String field, String content) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setThumbnail(red_excla_url);
        eb.setColor(color);
        eb.setTitle(title);
        eb.addField(field, content, false);
        return eb;
    }
    @SuppressWarnings("unchecked")
    public static EmbedBuilder queue(ArrayList<AudioTrack> arr, SlashCommandInteractionEvent event, int page, boolean recursive, int totalAdded) {
        EmbedBuilder eb = new EmbedBuilder();

        // Extremely unnecessary but why not
        String trackOrTracks = "tracks";
        if (arr.size() == 1) {
            trackOrTracks = "track";
        }
        eb.setTitle("Track Queue [" + arr.size() + " " + trackOrTracks + "]");
        ArrayList<AudioTrack> notAdded = (ArrayList<AudioTrack>) arr.clone();
        boolean overflow = false;
        long chars_used = 0;
        long page_count = 0;
        int index = 0;

        if (recursive) {
            index = totalAdded;
        }

        long total_duration = 0;
        for (AudioTrack track : arr) {
            total_duration = total_duration + track.getDuration();
            index++;
            String loop = "";

            String requestedBy = "";

            try {
                requestedBy = Objects.requireNonNull(MusicCommands.getPlaylist(
                                Objects.requireNonNull(event.getGuild()).getId()))
                        .requestedBy.get(track);
                chars_used += requestedBy.length();

                // Get the repeat value
                if(Objects.requireNonNull(MusicCommands.getPlaylist(event.getGuild().getId())).isLoop && index == 1) {
                    loop = ":repeat_one:";
                    chars_used += loop.length();
                }

            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            eb.addField(index + ". " + track.getInfo().title, Utility.msToMinuteSec(track.getDuration()) + " " + loop, true);
            eb.addField("Requested By", requestedBy, true);
            eb.addBlankField(true);
            notAdded.remove(track);
            if (eb.length() >= 5500 || eb.getFields().size() > 24) {
                System.out.println("Reached queue message limit, adding a page... " + eb.length());
                MusicCommands.getQueuePages(event).add(page, eb);
                queue(notAdded, event,page + 1, true, index);
                break;
            }
        }
        eb.setFooter("Duration: " + Utility.msToMinuteSec(total_duration));
        if (!recursive) {
            if (MusicCommands.getQueuePages(event).size() == 0) {
                return eb;
            }
            return MusicCommands.getQueuePages(event).get(page);
        }

        return eb;
    }
    public static EmbedBuilder help() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Wooden Beats Help Menu");
        eb.addField("help","Show available commands",false);
        eb.addField("beatedit","Change properties/beat of current track. Usage: TODO", true);
        eb.addField("clear","Clear the queue.", true);
        eb.addBlankField(true);
        eb.addField("forward", "Skip forward by 10 (default) seconds in a song. (Usage: forward 20)", true);
        eb.addField("leave", "Disconnect the bot from the current voice channel.", true);
        eb.addBlankField(true);
        eb.addField("loop", "Place a track or queue on repeat.", true);
        eb.addField("move", "Move a track to a new position in the queue. (Usage: move 5 2)", true);
        eb.addBlankField(true);
        eb.addField("nowplaying", "Display the track currently playing.", true);
        eb.addField("pause", "Pause the current track.", true);
        eb.addBlankField(true);
        eb.addField("play", "Add a track to the queue.", true);
        eb.addField("playtop", "Add a track to the top of the queue.", true);
        eb.addBlankField(true);
        eb.addField("reddit", "Display a post from a subreddit.", true);
        eb.addField("remove", "Remove a track from the queue.", true);
        eb.addBlankField(true);
        eb.addField("resume", "Resume any paused tracks.", true);
        eb.addField("rewind", "Rewind a song by 10 (default) seconds.", true);
        eb.addBlankField(true);
        eb.addField("search","Search for similar songs.", true);
        eb.addField("seek","Skip to a position in the track.", true);
        eb.addBlankField(true);
        eb.addField("shuffle", "Randomize queue order.", true);

        return eb;
    }
    public static EmbedBuilder botDisconnected() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Bot disconnecting from voice channel.");
        return eb;
    }

    public static EmbedBuilder noSession() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("No active session!");
        eb.addField("Type /play to start a song.", "", false);
        return eb;
    }

    public static EmbedBuilder player_botDisconnected() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Bot has been disconnected.");
        eb.addField("Use /play to play a new song!", "", false);
        return eb;
    }
    public static EmbedBuilder player_AddedPlaylistToQueue(String user, String source, AudioPlaylist playlist) {
        int counter = 0;

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Added playlist to queue!");
        eb.setColor(Color.GREEN);
        eb.addField("Title", playlist.getName(), false);
        eb.addField("Source", source, false);
        while (counter < 5 && counter < playlist.getTracks().size()) {
            try {
                counter++;
                eb.addField(counter + ". " + playlist.getTracks().get((counter - 1)).getInfo().title, "", false);
            } catch (Exception e) {
                System.err.println("Skipping song in respond message due to an exception.");
            }
        }
        if ((counter + 1) < playlist.getTracks().size()) {
            try {
                eb.addField("... and " + (playlist.getTracks().size() - counter) + " additional songs.", "", false);
            } catch (Exception e) {
                System.err.println("Skipping song in respond message due to an exception.");
            }
        }
        return eb;
    }


    public static EmbedBuilder player_addedTrackToQueue(String user, String source, String title) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Added song to queue!");
        eb.setColor(Color.GREEN);
        eb.addField("Title", title, true);
        eb.addField("Source", source, true);
        eb.addBlankField(true);
        eb.addField("Added By", user, false);
        return eb;
    }

    public static EmbedBuilder player_nowPlaying(String title, String returnBar, String durationTimer, String source, String requestedBy, String guildId, String youtubeURI) {
        EmbedBuilder eb = new EmbedBuilder();
        try {
            String trackURI = youtubeURI.replace("https://www.youtube.com/watch?v=", "");
            String thumbnailURI = "https://img.youtube.com/vi/" + trackURI + "/0.jpg";
            eb.setThumbnail(thumbnailURI);
            //eb.setThumbnail("https://cdn.discordapp.com/emojis/887875965705928764.gif?size=96&quality=lossless");
            // ----------------------Check if on loop-----------------------
            String loop = "";
            if(MusicCommands.getPlaylist(guildId).isLoop) {
                loop = ":repeat_one:";
            }
            else {
                loop = "";
            }
            // ------------------------------------------------------------
            // ----------------------Check if paused-----------------------
            String paused = ":arrow_forward:";
            if(MusicCommands.getPlayer(guildId).isPaused()) {
                paused = ":pause_button:";
            }
            else {
                paused = ":arrow_forward:";
            }
            // ------------------------------------------------------------

            eb.setTitle("Playing Track " + loop);
            eb.addField("Title", title + " " + paused, false);
            eb.addField(returnBar
                            + "\n" +
                            durationTimer,
                    "", false);
            eb.addField("Source", source, true);
            eb.addBlankField(true);
            eb.addField("Requested By", requestedBy, true);

            if (MusicCommands.getPlaylist(guildId).queue().size() == 1) {
                eb.addField("Next Up", "End of Queue", false);
            } else {
                eb.addField("Next Up", MusicCommands.getPlaylist(guildId).queue().get(1).getInfo().title, true);
                eb.addBlankField(true);
                eb.addField("Requested By", MusicCommands.getTrackRequester(MusicCommands.getPlaylist(guildId).queue().get(1), guildId), true);
            }
        } catch (Exception e) {
            eb = new EmbedBuilder();
            eb.setTitle("Playing Track");
            eb.addField("Identifier", "", true);
            eb.addField("Duration",
                    "00:00"
                    , true);
            eb.addBlankField(true);
            eb.addField("Title", "No Track Playing, use /play to start a song.", false);
        }
        return eb;
    }

    public static EmbedBuilder noNextSongError() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("End of Queue!");
        eb.addField("You've reached the end of the queue.", "To add a song, use /play", false);
        return eb;
    }
    public static EmbedBuilder skippingSong() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Skipping to next track.");
        return eb;
    }

    public static EmbedBuilder move(long from, long to, String guid) {
        Playlist playlist = MusicCommands.getPlaylist(guid);
        AudioTrack trackToMove = playlist.peekIndex((int) from);
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Moved Track #" + from + " -> #" + to);
        eb.addField("Title", trackToMove.getInfo().title, false);
        return eb;
    }

    public static EmbedBuilder genericException(Exception e) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("An exception occurred!");
        eb.addField(e.getMessage(),"",false);
        eb.addField(e.getStackTrace().toString(), "", false);
        return eb;
    }


}
