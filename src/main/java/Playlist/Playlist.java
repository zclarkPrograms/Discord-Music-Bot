package Playlist;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Playlist Class is used to handle and store our music queue.
 */

public class Playlist {
    public HashMap<AudioTrack, String> requestedBy = new HashMap<>();
    public HashMap<AudioTrack, LocalTime> requestTime = new HashMap<>();

    AudioTrack currently_playing;
    public ArrayList<AudioTrack> musicQueue = new ArrayList<AudioTrack>(200);
    int index;
    int index2;
    public boolean isLoop;

    //default constructor when creating a Playlist object
    public Playlist(){
        currently_playing = null;
        index = 0;
        index2 = 0;
        isLoop = false;
    }

    //Constructor that assigns a track to the musicQueue
    public Playlist(AudioTrack track) {
        currently_playing = track;
        musicQueue.add(track);
        index = 0;
        index2 = 0;
        isLoop = false;
    }

    public AudioTrack peekIndex(int i) {
        return musicQueue.get(i);
    }

    //sets currentlyplaying to the track that is passed
    public void setCurrentlyPlaying(AudioTrack track) {
        this.currently_playing = track;
    }

    //nowplaying method returns the current song playing, getter for current_playing.
    public AudioTrack nowPlaying() {
        return currently_playing;
    }

    //play method adds the track passed to the end of music queue
    public void play(AudioTrack track) {
        System.out.println("Adding track " + track.getInfo().title + " to queue. #" + musicQueue.size());
        musicQueue.add(track);
    }

    //0th position in queue is always going to be the current song playing
    //playtop method adds the track passed to the top of the music queue (1st position)
    public void playtop(AudioTrack track){

        System.out.println("Adding track to top of the queue " + track.getInfo().title + " to queue. #1");
        musicQueue.add(1, track);
    }

    /**isloop global var and have check to see if its on and if so just return the same song instead of going next.
     to be called when current song is over, this will change currently_playing to the next song which is the song in
     the 1th position of the musicQueue. The 0th position of the queue is the current song playing.**/
    public AudioTrack next(){
        try {
            System.out.println("In next");
            if (isLoop) {
                return currently_playing;
            } else {
                currently_playing = musicQueue.get(1);
                musicQueue.remove(0);
            }
            System.out.println("Playing next track " + currently_playing.getInfo().title);
            return currently_playing;
        } catch (IndexOutOfBoundsException e) { // If there is no track next up in queue
            return null;
        }
    }

    //probably track by index
    //currently not implemented
    public AudioTrack previous(){
        return null;
    }

    //queue method returns the musicQueue so that any information needed can be accessed
    public ArrayList<AudioTrack> queue() {
        return musicQueue;
    }

    //skip method skips the current song and goes to the next song in the queue
    public AudioTrack skip(){
        currently_playing = musicQueue.get(1);
        musicQueue.remove(0);

        return currently_playing;
    }

    //skipto method skips the current song and goes to the song specified by index
    //move the indexed song up to the front
    public AudioTrack skipto(int index){
        currently_playing = musicQueue.get(index);
        musicQueue.set(0, musicQueue.get(index));
        musicQueue.remove(index);

        return currently_playing;
        /**
         //if index is not a valid index aka its negative or bigger than the queue then return null
         //gonna need to figure out a better way to check length
         if((index >= 0) && (index < musicQueue.length)){
         temp = musicQueue[index];
         }else{
         return null;
         }**/
    }

    //maybe figure out how to do loop for single song and queue loop ( 1 is single 2 is queue)
    //look up how to randomize an array/arraylist
    //loop method loops the queue either for a single song or for the entire queue depending on the option
    //not fully implemented
    public boolean loop(){
        isLoop = !isLoop; // If loop is on, turn it off. If off, turn on.
        if (isLoop) {
            return true;
        }
        return false;
    }

    //shuffle method randomizes the music queue
    public void shuffle(){
        // (Gursharn) finished shuffle implementation
        AudioTrack currentTrack = musicQueue.get(0);
        musicQueue.remove(0);
        Collections.shuffle(musicQueue);
        musicQueue.add(0, currentTrack);
    }

    //clear method clears the music queue so that it is empty
    public void clear(){
        musicQueue.clear();
    }

    //if removed then i have to move everything over to the left
    //remove method removes the song in the specified index
    //might need to check for case when they try to remove 0th position (also check other methods)
    public void remove(int index){
        musicQueue.remove(index - 1);
    }

    //move method moves the song in the from position to the to position
    public void move(int from, int to){
        AudioTrack temp = musicQueue.get(from);

        musicQueue.set(from, musicQueue.get(to));
        musicQueue.set(to, temp);
    }

    public void testing() {
        int temp = 1;
        System.out.println(temp);
    }

    public static void main(String[] args) {

    }
}