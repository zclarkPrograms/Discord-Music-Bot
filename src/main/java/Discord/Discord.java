package Discord;

import BeatEdit.BeatEdit;
import Playlist.AudioPackage;
import Playlist.Playlist;
import Reddit.Reddit;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.AudioChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Discord extends ListenerAdapter {
    private static JDA jda; // all of discord
    private static Guild guild; // specific discord server
    private static int activeMessages = 0;
    private static Reddit reddit;
    public static Exception exceptionToMessage;

    private static final int TOTAL_FIELDS = 6;

    public static void main(String[] args) {
        // Building JDA
        try {
            List<String> info = getInfo();

            System.out.println("Authenticating with Discord...");
            setupJDA(info.get(0), info.get(1));

            reddit = getRedditClient(info.get(2), info.get(3), info.get(4), info.get(5));
        } catch (LoginException failedLogin) {
            System.err.println("Discord Authentication failed");
            failedLogin.printStackTrace();
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
    }

    public static List<String> getInfo(){
        List<String> info = new ArrayList<>();

        try(Scanner sc = new Scanner(new File("info.txt"))){
            sc.useDelimiter("$");
            info = sc.next()
                    .lines()
                    .map(x -> x.replaceAll("^.*?:\\s*", ""))
                    .collect(Collectors.toList());

            if(info.size()<TOTAL_FIELDS){
                System.err.println("Not enough information was provided");
                System.out.println(info.size());
                System.exit(1);
            }

            return info;
        } catch (FileNotFoundException ex){
            System.err.println("Information file could not be found");
            System.exit(1);
        }

        return info;
    }

    public static Reddit getRedditClient(String username, String password, String clientID, String clientSecret){
        return new Reddit(username, password, clientID, clientSecret);
    }
    public static Reddit getRedditClient(){
        List<String> info = getInfo();

        return getRedditClient(info.get(2), info.get(3), info.get(4), info.get(5));
    }

    public static void setupJDA(String apiToken, String guildID) throws LoginException, InterruptedException{
        // Discord bot token found in Discord developer portal
        jda = JDABuilder.createDefault(apiToken)
                .setMemberCachePolicy(MemberCachePolicy.ALL) // cache all members
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new Discord()).build();
        jda.awaitReady(); // Delay to allow guild to be initialized
        jda.getPresence().setPresence(Activity.streaming(" for " + activeMessages + " server(s)", ""), false);
        guild = jda.getGuildById(guildID);
        // Building commands
        assert guild != null;
        jda.updateCommands().addCommands(
                Commands.slash("debug", "Test command"),
                Commands.slash("help", "List all commands"),
                Commands.slash("clear", "Clear the queue"),
                Commands.slash("leave", "Disconnect the bot"),
                Commands.slash("skip", "Play next song"),
                Commands.slash("skipto", "skip to specified song")
                        .addOption(OptionType.INTEGER, "index", "position in queue", true),
                Commands.slash("pause", "Pause the current song"),
                Commands.slash("previous", "Play previous song"),
                Commands.slash("nowplaying", "Display the track currently playing"),
                Commands.slash("shuffle", "Shuffle the queue"),
                Commands.slash("beatedit", "Edit the beats of a song")
                        .addOption(OptionType.STRING, "filename", "Name of file to edit"),
                Commands.slash("forward", "Skip forward by 10 (default) seconds"),
                Commands.slash("loop", "Repeat a track or queue"),
                Commands.slash("move", "Move a track to a new position in queue")
                        .addOption(OptionType.INTEGER, "from", "Track to move",true)
                        .addOption(OptionType.INTEGER, "to", "New position in queue", true),
                Commands.slash("playtop", "Add a track on the top of the queue")
                        .addOption(OptionType.STRING, "name", "Search Query"),
                Commands.slash("remove", "Remove a track from the queue")
                        .addOption(OptionType.STRING, "index", "Index to remove."),
                Commands.slash("resume", "Resume any paused track"),
                Commands.slash("rewind", "Rewind a song by 10 (default) seconds"),
                Commands.slash("search", "Search for similar songs"),
                Commands.slash("seek", "Skip to a position in the track"),
                Commands.slash("reddit", "Searches for posts on a subreddit")
                        .addOption(OptionType.STRING, "name", "Subreddit"),
                Commands.slash("queue", "Display the queue"),
                Commands.slash("play", "Search & play a track")
                        .addOption(OptionType.STRING, "name", "Search Query")

        ).queue();
    }

    public static JDA getJDAInstance() {
        return jda;
    }
    public static Guild getGuildInstance() {
        return guild;
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder(); // Embed Message builder
        try {
            // Reddit Commands
            if (event.getName().equals("reddit")) {
                event.deferReply().queue();

                if (event.getOption("name") == null) {
                    event.getHook().sendMessageEmbeds(EmbedReplies.genericErrorBuilder(
                            "No Subreddit Provided!", Color.ORANGE,
                            "A subreddit must be provided to use this command", ""
                    ).build()).queue();

                    return;
                }

                reddit.process(event.getChannel(), "r/" + event.getOption("name").getAsString());
                event.getHook().sendMessage("Done").queue();
            }
            // --------------------
            if (event.getName().equals("beatedit") && !BeatEdit.process(event)) {
                return;
            }

            //clears queue but need to clean up so its more user friendly in response/error handling
            if (event.getName().equals("clear")) {
                MusicCommands.clear(event);
                System.out.println("test");
            }

            if (event.getName().equals("debug")) {
                // todo
            }

            if (event.getName().equals("remove")) {
                Playlist p = MusicCommands.getPlaylist(event.getGuild().getId());
                if (event.getOption("index").getAsLong() == 1 || event.getOption("index").getAsLong() == 0) {
                    event.reply("Cannot remove active song!").queue();
                    return;
                }
                p.remove((int) event.getOption("index").getAsLong());
                event.reply("Removed song #" + event.getOption("index").getAsString()).queue();
            }

            if (event.getName().equals("playtop")) {
                event.deferReply().queue();
                if (event.getOption("name") == null) {
                    event.getHook().sendMessageEmbeds(EmbedReplies.genericErrorBuilder(
                            "No Song Provided!", Color.ORANGE,
                            "A song must be provided to use this command", ""
                    ).build()).queue();
                    return;
                }
                String search = event.getOption("name").getAsString();
                AudioChannel channel = play_findAudioChannel(event);
                AudioPackage playlistExists = MusicCommands.play(event, channel, search, true);
                System.out.println("Playlist exists value :: " + playlistExists);
                if (playlistExists == null) {
                    generateNowPlaying(event);
                }
            }

            if (event.getName().equals("loop")) {
                boolean isLoop = MusicCommands.loop(event.getGuild().getId());
                if (isLoop) {
                    event.reply("Looping playback!").queue();
                } else {
                    event.reply("Unlooping playback!").queue();
                }
            }


            if (event.getName().equals("shuffle")) {
                // TODO Add embedded message reply.
                try {
                    MusicCommands.getPlaylist(event.getGuild().getId()).shuffle();
                    event.reply("Shuffled Playlist!").queue();
                } catch (NullPointerException e) {
                    event.reply("Error shuffling playlist").queue();
                    e.printStackTrace();
                }
            }


            if (event.getName().equals("queue")) {
                if (MusicCommands.getQueue(event) == null || MusicCommands.getQueue(event).clone() == null) {
                    event.replyEmbeds(EmbedReplies.noSession().build()).queue();
                    return;
                }
                ArrayList<AudioTrack> arr = (ArrayList<AudioTrack>) MusicCommands.getQueue(event).clone();
                assert arr != null;
                EmbedBuilder queue = EmbedReplies.queue(arr, event, 0, false, 0);
                if (MusicCommands.isOverflowedQueue(event)) {
                    event.replyEmbeds(queue.build()).addActionRow(
                            Button.primary("nextpage", "Next Page")).queue();
                }
                else {
                    event.replyEmbeds(queue.build()).queue();
                }
            }

            if (event.getName().equals("skip")) {
                int res = MusicCommands.skip(event);
                if (res == -1) {
                    event.replyEmbeds(EmbedReplies.noNextSongError().build()).queue();
                } else {
                    event.replyEmbeds(EmbedReplies.skippingSong().build()).queue();
                }
            }

            //need to fix proper checks with indexing and interaction between discord.java,
            //musiccommands.java, and playlist.java
            if (event.getName().equals("skipto")) {
                long index = event.getOption("index").getAsLong();
                index--;
                int res = MusicCommands.skipto(event, (int) index);

                if(res == -1) {
                    event.replyEmbeds(EmbedReplies.noNextSongError().build()).queue();
                } else {
                    event.replyEmbeds(EmbedReplies.skippingSong().build()).queue();
                }
            }

            if (event.getName().equals("leave")) {
                boolean success = AudioStream.destroyAudioPlayer(event);
                System.out.println("/leave used in Guild " + event.getGuild().getId());
                if (success) {
                    event.replyEmbeds(EmbedReplies.botDisconnected().build()).queue();
                } else {
                    event.replyEmbeds(EmbedReplies.genericException(exceptionToMessage).build()).queue();
                }


            }

            if (event.getName().equals("nowplaying")) {
                event.deferReply().queue();
                // Kill any previous now playing messages. Do this for rate limiting purposes
                MusicCommands.killExistingMessages(event.getGuild().getId());
                // TODO this is not working properly. sometimes the message produced by nowplaying will be disconnected instead. Need to change how it works so only the old player gets stopped.

                try {
                    Thread.sleep(1500); //TODO Band-aid fix for the demo.
                    generateNowPlaying(event);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            if (event.getName().equals("play")) {
                System.out.println("Play command");
                event.deferReply().queue();
                if (event.getOption("name") == null) {
                    event.getHook().sendMessageEmbeds(EmbedReplies.genericErrorBuilder(
                            "No Song Provided!", Color.ORANGE,
                            "A song must be provided to use this command", ""
                    ).build()).queue();
                    return;
                }
                String search = event.getOption("name").getAsString();
                AudioChannel channel = play_findAudioChannel(event);
                AudioPackage playlistExists = MusicCommands.play(event, channel, search, false);
                System.out.println("Playlist exists value :: " + playlistExists);

            }

            if (event.getName().equalsIgnoreCase("help")) {
                event.replyEmbeds(EmbedReplies.help().build()).queue();
            }
            if (event.getName().equalsIgnoreCase("move")) {
                long from = event.getOption("from").getAsLong();
                long to = event.getOption("to").getAsLong();
                MusicCommands.getPlaylist(event.getGuild().getId()).move((int) from, (int) to);
                event.replyEmbeds(EmbedReplies.move(from, to, event.getGuild().getId()).build()).queue();
            }
        } catch (Exception e) {
            try {
                e.printStackTrace();
                event.replyEmbeds(EmbedReplies.genericException(e).build()).queue();
            } catch(IllegalStateException alreadyReplied) {
                System.err.println("Exception handler already replied to message");
                //event.getChannel().sendMessageEmbeds(EmbedReplies.genericException(e).build()).queue();
            }
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (event.getMember().getUser().getId().equalsIgnoreCase(event.getJDA().getSelfUser().getId())) {
            MusicCommands.shutdown(event.getGuild().getId());
        }
        if (event.getChannelLeft().getMembers().size() == 1) {
            System.out.println("Channel is empty... ending session.");
            MusicCommands.shutdown(event.getGuild().getId());
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("pause")) {
            System.out.println("Pausing playback");

            MusicCommands.pause(event);
            event.editButton(Button.primary("resume","Resume")).queue();

        }
        if (event.getComponentId().equals("resume")) {
            System.out.println("Resuming playback");
            MusicCommands.resume(event);
            event.editButton(Button.primary("pause","Pause")).queue();
        }
        if(event.getComponentId().equals("next")){
            System.out.println("Advancing to next song");
            if(MusicCommands.skip(event)>-1){
                event.editMessage("").queue();
            }
            else{
                event.reply("There are no more songs after this one.").queue();
            }
        }
        if (event.getComponentId().equals("nextpage")) {
            System.out.println("Getting next page in queue");
            EmbedBuilder eb = MusicCommands.getNextQueuePage(event);
            event.editMessageEmbeds(eb.build()).queue();
        }
        if (event.getComponentId().startsWith("r/")) {
            event.deferEdit().queue();
            reddit.process(event.getChannel(), event.getButton().getId());
        }

    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            if (event.getMessage().getContentRaw().equals("!clear")) {
                // ijsdofijsd
            }
        }
        EmbedBuilder eb = new EmbedBuilder();
        if (event.getMessage().getContentRaw().equals("!a")) {
            System.out.println("Msg received");
            eb.setTitle("**It's alive!**","http://www.google.com");
            //AudioStream.audioPlayer("https://www.youtube.com/watch?v=5sNuDu4dE8Y", null, );
            event.getChannel().sendMessageEmbeds(eb.build()).queue();
        }
        if (event.getMessage().getContentRaw().equals("!eli")) {
            System.out.println("it worked!");
            event.getChannel().sendMessage("It works!").queue();
        }

        if(event.getMessage().getContentRaw().equals("!play")){
            System.out.println("2");

        }

        if(event.getMessage().getContentRaw().equals("!playtop")){
            System.out.println("3");

        }

        if(event.getMessage().getContentRaw().equals("!skip")){
            System.out.println("4");

        }

        if(event.getMessage().getContentRaw().equals("!shuffle")){
            System.out.println("5");

        }
        /*
        if(event.getMessage().getContentRaw().equals("!queue")){
            System.out.println("6");
            if(MusicCommands.getQueue(event) == null || MusicCommands.getQueue(event).clone() == null) {
                event.replyEmbeds(EmbedReplies.noSession().build()).queue();
            }

        }*/

        if(event.getMessage().getAuthor()!=jda.getSelfUser()){
            reddit.process(event.getChannel(), event.getMessage().getContentRaw().replace("!reddit ", "r/"));
        }
    }


    public static void generateNowPlaying(SlashCommandInteractionEvent event) {
        EmbedBuilder eb = new EmbedBuilder();
        AudioTrack currently_playing = MusicCommands.getPlayer(event.getGuild().getId()).getPlayingTrack();
        System.out.println("Currently Playing: " + currently_playing);
        System.out.println(MusicCommands.getPlaylist(event.getGuild().getId()).requestedBy.toString());
        String requestedBy = MusicCommands.getTrackRequester(currently_playing, event.getGuild().getId());
        System.out.println("Requested By: " + requestedBy);
        eb.setTitle("Playing Track");
        eb.addField("Title", MusicCommands.getPlayer(event.getGuild().getId()).getPlayingTrack().getInfo().title, false);
        eb.addField("Duration", String.valueOf(MusicCommands.getPlayer(event.getGuild().getId()).getPlayingTrack().getDuration()), false);


        eb.addField("Source", currently_playing.getSourceManager().getSourceName(), true);
        eb.addBlankField(true);
        eb.addField("Requested By", requestedBy, true);

        eb.addField("_______________________________", "", false);
        if (MusicCommands.getPlaylist(event.getGuild().getId()).queue().size() == 1) {
            eb.addField("Next Up", "End of Queue", false);
        } else {
            eb.addField("Next Up", MusicCommands.getPlaylist(event.getGuild().getId()).queue().get(1).getInfo().title, false);
        }
        event.getHook().sendMessageEmbeds(eb.build())
                .addActionRow(Button.primary("pause", "Pause"),
                        Button.primary("next", "Next")
                ).queue(
                        message -> {
                            activeMessages++;
                            jda.getPresence().setPresence(Activity.playing(" for " + activeMessages + " server(s)"), false);
                            //message.addReaction("\uD83D\uDC4D").queue();
                            //message.addReaction("\uD83D\uDC4E").queue();
                            while (true) {
                                // TODO Disable message updates if it causes performance issues.

                                if (!MusicCommands.isActive(message.getGuild().getId()) || MusicCommands.isTimedOut(message.getGuild().getId())) {
                                    activeMessages--;
                                    jda.getPresence().setPresence(Activity.playing(" for " + activeMessages + " server(s)"), false);
                                    System.out.println("! --- Stopping updates to messages, bot no longer active in server.");
                                    if (MusicCommands.isTimedOut(message.getGuild().getId())) {
                                        System.out.println("Bot timed out due to inactivity, shutting down!");
                                        MusicCommands.shutdown(message.getGuild().getId()); // Shut the bot down if it's timed out
                                    }
                                    message.editMessageEmbeds(EmbedReplies.player_botDisconnected().build()).queue();
                                    break; // This will return null if another message is created. We only want to update one at a time.
                                }
                                else if (MusicCommands.messageReplaced(message.getGuild().getId(), false)) {
                                    System.out.println("Message has been replaced. Stopping message updates.");
                                    activeMessages--;
                                    message.editMessageEmbeds(EmbedReplies.player_botDisconnected().build()).queue();

                                    break;
                                }
                                EmbedBuilder updater = PlayerMenu.updatePlayer(MusicCommands.getPlayer(message.getGuild().getId()), message.getId(), message.getGuild().getId());

                                message.editMessageEmbeds(updater.build()).queue();
                                try {
                                    if (activeMessages >= 10) {
                                        // Help with rate limiting, if there's more than 10 messages, double the update rate
                                        Thread.sleep(2000);
                                    } else {
                                        Thread.sleep(1000);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                );
    }

    public static AudioChannel play_findAudioChannel(SlashCommandInteractionEvent event) {
        AudioChannel channel = null;
        String channelId = "";
        try {
            channelId = event.getMember().getVoiceState().getChannel().getId();
            channel = event.getMember().getVoiceState().getChannel().getGuild().getVoiceChannelById(channelId);
            System.out.println("Channel ID: " + channel.getId());

        } catch (Exception e) {
            System.out.println("User is not in a voice channel!");
            event.getHook().sendMessageEmbeds(EmbedReplies.genericErrorBuilder
                            ("User is not in a voice channel!",
                                    Color.ORANGE, "You must be in a voice channel to use this command.", "")
                    .build()).queue();
            return null;
        }
        return channel;
    }



}
