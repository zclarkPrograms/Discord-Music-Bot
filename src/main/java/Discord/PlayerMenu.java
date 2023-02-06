package Discord;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;


public class PlayerMenu {
    public static EmbedBuilder updatePlayer(AudioPlayer player, String messageId, String guildId) {
        EmbedBuilder eb = new EmbedBuilder();
        AudioTrack currently_playing = MusicCommands.getPlaylist(guildId).nowPlaying();
        String requestedBy = MusicCommands.getTrackRequester(currently_playing, guildId);
        try {
            eb = EmbedReplies.player_nowPlaying(
                    player.getPlayingTrack().getInfo().title,
                    returnBar(
                            // Build percentage bar
                            getPercentageDone
                                    (player.getPlayingTrack().getPosition(),
                                            player.getPlayingTrack().getDuration(),
                                            60)),
                    Utility.msToMinuteSec(player.getPlayingTrack().getPosition()) + " / " + Utility.msToMinuteSec(player.getPlayingTrack().getDuration()),
                    currently_playing.getSourceManager().getSourceName(),
                    requestedBy,
                    guildId, player.getPlayingTrack().getInfo().uri
            );
        } catch (NullPointerException e) {
            System.out.println("updatePlayer() no track detected");
            MusicCommands.incrementTimeout(guildId);
            // Get the no track playing Embed from player_nowPlaying
            eb = EmbedReplies.player_nowPlaying(null, null, null, null, null, null, null);
        }
        return eb;
    }

    // This will return the number of bars to return to fill the progress bar
    private static long getPercentageDone(long prog, long dur, int barLength) {
        float result = ((float)prog/(float)dur) * 30;
        return (long) result;
    }
    private static String returnBar(long bars) {
        // Building the progress bar, there are 30 'empty' squares
        StringBuilder builder = new StringBuilder("\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1\u25A1");
        for (int i = 0; i <= bars; i++) {
            builder.setCharAt(i, '\u25A0'); // set the square to 'filled'
        }
        return builder.toString();
    }
}

