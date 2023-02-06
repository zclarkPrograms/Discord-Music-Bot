package Playlist;


import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

/**
 * Bridges the gap between our playlist manager and the Discord audio stream.
 * This class' template is from the LavaPlayer documentation.
 */
public class Scheduler extends AudioEventAdapter {
    private AudioPlayer player;
    private AudioTrack last_played;
    private String guid;
    private Playlist playlist;
    public AudioPackage pkg;

    public Scheduler(AudioPlayer player, String guid, AudioPackage pkg) {
        this.guid = guid;
        this.player = player;
        this.playlist = pkg.playlist;

    }


    @Override
    public void onPlayerPause(AudioPlayer player) {
        System.out.println("Pausing track");
        player.setPaused(true);
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        System.out.println("onPlayerResume");
        player.setPaused(false);
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        // A track started playing
        System.out.println("Playing track " + track.getInfo().title);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (!endReason.mayStartNext) {
            return;
        }
        System.out.println("onTrackEnd");
        AudioTrack nextTrack = pkg.playlist.next();
        if (nextTrack != null) {
            pkg.player.playTrack(nextTrack);
        }

        // Start next track

        // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
        // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
        // endReason == STOPPED: The player was stopped.
        // endReason == REPLACED: Another track started playing while this had not finished
        // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
        //                       clone of this back to your queue
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        // An already playing track threw an exception (track end event will still be received separately)
        System.out.println("exception");
        exception.printStackTrace();

    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        // Audio track has been unable to provide us any audio, might want to just start a new track
        System.out.println("stuck");

    }
}
