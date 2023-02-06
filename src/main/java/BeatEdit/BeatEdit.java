package BeatEdit;

import Discord.Discord;
import Discord.EmbedReplies;
import Discord.MusicCommands;
import Playlist.AudioPackage;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class BeatEdit {
    private static final String PATH=System.getProperty("user.dir")+"/src/main/java/BeatEdit/";

    public static boolean process(SlashCommandInteractionEvent event){
        String filename=PATH+event.getOption("filename").getAsString();
        EmbedBuilder eb=new EmbedBuilder();
        System.out.println(PATH);
        if(new File(filename).exists()){
            String channelId="";
            VoiceChannel channel=null;

            try {
                channelId = event.getMember().getVoiceState().getChannel().getId();
                channel = event.getMember().getVoiceState().getChannel().getGuild().getVoiceChannelById(channelId);
            } catch (Exception e) {
                System.out.println("User is not in a voice channel!");
                return false;
            }

            event.deferReply().queue();

            if(!new File(filename.replace(".mp3", "_edited.mp3")).exists()){
                try {
                    ProcessBuilder pb = new ProcessBuilder("python", PATH+"beatedit.py", filename);
                    Process p = pb.start();

                    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    event.getChannel().sendMessage("This will take a while. Please wait in an audio channel.").queue();
                    System.out.println(in.readLine());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            AudioPackage playlistExists = MusicCommands.play(event, channel, filename.replace(".mp3", "_edited.mp3"), false);

            System.out.println("Playlist exists value :: " + playlistExists);
            if (playlistExists == null) {
                Discord.generateNowPlaying(event);
            }
            else {
                event.getHook().sendMessageEmbeds(
                        EmbedReplies.player_addedTrackToQueue(event.getUser().getName(),
                                playlistExists.handler.track.getSourceManager().getSourceName(),
                                playlistExists.handler.track.getInfo().title).build()).queue();
            }

            eb.setTitle("Playing Track");
            eb.addField("Identifier", event.getOption("filename").getAsString(), true);
            eb.addField("Duration", String.valueOf(MusicCommands.getPlayer(event.getGuild().getId()).getPlayingTrack().getDuration()), true);

            event.getHook().sendMessageEmbeds(eb.build()).queue();
        }
        else{
            System.out.println("File could not be found.");

            event.getHook().sendMessageEmbeds(EmbedReplies.genericErrorBuilder(
                    "File could not be found!!", Color.ORANGE,
                    "A valid file must be provided to use this command", ""
            ).build()).queue();

            return false;
        }

        return true;
    }
}
