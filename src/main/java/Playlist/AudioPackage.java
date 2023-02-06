package Playlist;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.ArrayList;
import java.util.HashMap;


public class AudioPackage {
    public LoadResultHandler handler;
    public AudioPlayerManager stream;
    public AudioManager manager;
    public Playlist playlist;
    public AudioPlayer player;
    public Scheduler scheduler;
    public String serverID;
    public ArrayList<EmbedBuilder> queuePages = new ArrayList<>();
    public HashMap<String, Integer> queueMessagePageTracker = new HashMap<>();
    public int staleTimer = 0; // How long this audio package has been CONNECTED but stale (i.e. no tracks playing/empty queue).
}
