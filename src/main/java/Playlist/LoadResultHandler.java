package Playlist;

import Discord.Utility;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.*;

public class LoadResultHandler implements AudioLoadResultHandler {
    public AudioPlaylist playlist;
    public AudioTrack track;

    // Blacklist any keywords here
    private static ArrayList<String> blacklist = new ArrayList<>(Arrays.asList(
       "music video", "acoustic", "official video", "8d audio","12d audio", "live"
    ));

    // Preferred keywords
    private static ArrayList<String> preferred = new ArrayList<>(Arrays.asList(
       "audio"
    ));


    /**
     * Called when the requested item is a track and it was successfully loaded.
     *
     * @param track The loaded track
     */
    @Override
    public void trackLoaded(AudioTrack track) {
        System.out.println("Loaded track!");
        this.track = track;
        System.out.println("Duration: " + track.getDuration());
    }






    /**
     * Called when the requested item is a playlist and it was successfully loaded.
     * We should make our own playlist/class system so we have more control & understanding.
     * @param playlist The loaded playlist
     */
    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        LinkedHashMap<AudioTrack, Integer> rating = new LinkedHashMap<>();
        System.out.println("Loaded playlist!");
        this.playlist = playlist;

        if (playlist.isSearchResult()) {
            System.out.println("Gathered search results!");
            // Filter out blacklisted words
            for (AudioTrack t : playlist.getTracks()) {
                int pref_matches = 0;
                pref_matches = Utility.containsAnyString(t.getInfo().title, blacklist) * -2;
                pref_matches = pref_matches + Utility.containsAnyString(t.getInfo().title, preferred);
                System.out.println(t.getInfo().title + " :: " + pref_matches);
                rating.put(t, pref_matches);
            }
            if (rating.size() > 0) {
                // Use Collections.max to parse the hashmap entry set and grab highest value->key
                this.track = Collections.max(rating.entrySet(), Map.Entry.comparingByValue()).getKey();
            } else {
                this.track = playlist.getTracks().get(0); // otherwise just grab the first result
            }
        }
        else { // Playlist is given by user.
            this.track = playlist.getTracks().get(0);
        }



    }

    /**
     * Called when there were no items found by the specified identifier.
     */
    @Override
    public void noMatches() {
        System.out.println("No matches found");

    }

    /**
     * Called when loading an item failed with an exception.
     *
     * @param exception The exception that was thrown
     */
    @Override
    public void loadFailed(FriendlyException exception) {
        System.out.println("Load failed!");
        exception.printStackTrace();

    }
}
