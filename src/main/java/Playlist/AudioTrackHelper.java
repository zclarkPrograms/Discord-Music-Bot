package Playlist;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;

/**
 *
 */
public class AudioTrackHelper {
    AudioTrack track; // the track this wrapper is holding
    public int id; // wrapper ID to help with organizing

    public AudioTrackHelper(AudioTrack track, int id) {
        this.id = id;
        this.track = track;
    } // Constructor


    public String getTitle() {
        return track.getInfo().title;
    }
    public String getAuthor() {
        return track.getInfo().author;
    }
    public long getDurationInMinutes() {
        return track.getDuration() / 60;
    }
    public String getSource() {
        return track.getSourceManager().getSourceName();
    }
    public AudioTrackState getState() {
        return track.getState();
    }
}
