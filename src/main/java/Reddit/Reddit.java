package Reddit;

import net.dean.jraw.models.Comment;
import net.dean.jraw.models.PublicContribution;
import net.dean.jraw.tree.CommentNode;
import net.dean.jraw.tree.RootCommentNode;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Reddit {
    private final HashMap<String, ArrayList<Post>> subreddits;
    private final HashMap<String, Message> messages; //used for keeping track of messages across multiple channels

    private SearchReddit searchReddit;

    public Reddit(String username, String password, String clientID, String clientSecret){
        subreddits=new HashMap<>();
        messages=new HashMap<>();
        searchReddit = new SearchReddit(username, password, clientID, clientSecret);
    }

    /**
     * Selects random post from list of posts from subreddit
     * @param subreddit subreddit from which to retrieve post
     * @return random post from subreddit
     */
    public Post getRandomPost(String subreddit){
        Random random=new Random();

        ArrayList<Post> posts=subreddits.get(subreddit);

        return posts.get(random.nextInt(posts.size()));
    }

	/**
	 * counts number of newline characters in comment.
     * A comment with many newline characters may be too large, even if there are few total characters in the comment.
     * @param str content of comment
     * @return number of newline characters in string
     */
    private int count(String str){
        int num=0;

        for(int i=0;i<str.length();i++){
            if(str.charAt(i)=='\n'){
                num++;
            }
        }

        return num;
    }

    /**
     * checks whether the comment is too long or is likely to be an invalid comment, such as a deleted comment
     * @param body content of comment
     * @return whether comment should be removed from comment list
     */
    private boolean shouldFilter(String body){
        return body.startsWith("![") ||
                body.startsWith("[") ||
                body.length()>500 ||
                count(body)>=3;
    }

    /**
     * Retrieves comments from the post, selecting the two most upvoted posts.
     * @param id id of post
     * @return the comments from the post
     */
    public ArrayList<RedditComment> getComments(String id){
        RootCommentNode root=searchReddit.getRedditClient().submission(id).comments();

        ArrayList<RedditComment> nodes=new ArrayList<>();

        // gets comments that are a direct reply to post
        for(CommentNode<Comment> node : root.getReplies()){
            PublicContribution<?> comment=node.getSubject();
            String body=node.getSubject().getBody();
            nodes.add(new RedditComment(comment.getScore(), comment.getAuthor(), body));
        }
		
		nodes.removeIf(comment -> (shouldFilter(comment.getBody()))); //removes comments that would be too long or don't contain useful text

        //selects the two most upvoted posts
        nodes.sort((comment1, comment2) -> comment2.getScore() - comment1.getScore());

        nodes=new ArrayList<>(nodes.size()>=2 ? nodes.subList(0, 2) : nodes);

        nodes.removeIf(comment -> (comment.getScore()<1)); //this makes sure that only comments with a positive number of upvotes are shown to the user

        return nodes;
    }

    /**
     * Displays the post information to the user in a message.
     * If the message has no messages after it, it will be edited with a new post, instead of creating a new message. This is for the user's convenience.
     * If a new message has been made, previous messages with these subreddit posts will have their next button disabled, so the user won't be able to use them to advance through posts on that subreddit.
     * @param channel Channel in which command was used
     * @param eb Message builder which is used to construct the post's information into a message
     * @param subreddit The requested subreddit
     */
    public void sendPost(MessageChannel channel, EmbedBuilder eb, String subreddit){
        Post post=getRandomPost(subreddit);
        ArrayList<RedditComment> comments=getComments(post.getId());
        RedditComment comment1=comments.size()>=1 ? comments.get(0) : null; //makes sure there is a comment in the first slot
        RedditComment comment2=comments.size()>=2 ? comments.get(1) : null; //makes sure there is a comment in the second slot
        String comment1_str="", comment2_str="";

        //makes sure each comment isn't null before displaying the comments' information
        if(comment1!=null){
            comment1_str=String.format("> :small_red_triangle:%d __%s__\n> %s\n\n", comment1.getScore(), comment1.getAuthor(), comment1.getBody().replace("\n", "\n> "));
        }

        if(comment2!=null){
            comment2_str=String.format("> :small_red_triangle:%d __%s__\n> %s\n", comment2.getScore(), comment2.getAuthor(), comment2.getBody().replace("\n", "\n> "));
        }

        eb.setTitle(post.getTitle(), post.getPermaUrl())
                .setColor(Color.red)
                .setDescription(String.format("%s%s", comment1_str, comment2_str))
                .setImage(post.getUrl())
                .setFooter(String.format("Posted in r/%s by u/%s", post.getSubreddit(), post.getAuthor()));

        MessageEmbed embed=eb.build();

        //checks how many messages in the channel are after tbe current post
        int numMessages=1;
        Message currentMessage=messages.get(channel.getId());

        if(currentMessage!=null){
            numMessages=MessageHistory.getHistoryAfter(channel, currentMessage.getId()).limit(1).complete().size();
        }

        MessageAction action;

        if(numMessages == 0) {
            action=currentMessage.editMessageEmbeds(embed);
        }
        else{
            action=channel.sendMessageEmbeds(embed);

            //if a new message was made for the current post, the next button on the previous message is disabled.
            if(currentMessage!=null){
                currentMessage.editMessage(currentMessage).setActionRow(Button.primary("r/" + subreddit, "Next").asDisabled()).queue();
            }
        }

        //creates a "next" button for the message, which is used to advance through posts in the subreddit.
        action.setActionRow(Button.primary("r/" + subreddit, "Next")).queue(message -> messages.put(channel.getId(), message));
    }

    /**
     * Processes the command the user sent
     * @param channel Message in which the command was used
     * @param message contents of the message
     */
    public void process(MessageChannel channel, String message){
        EmbedBuilder eb = new EmbedBuilder();
        ArrayList<Post> posts;

        if(message==null || !message.startsWith("r/")){
            return;
        }

        String subreddit=message.substring(2);

        //retrieves posts from the subreddit on reddit if this subreddit wasn't requested before
        //these posts are then placed inside a hashmap for storage
        if(subreddits.get(subreddit)==null){
            //channel.sendMessage(String.format("Retrieving posts from r/%s", subreddit)).queue();

            posts=searchReddit.getPosts(subreddit);

            //makes sure subreddit is valid
            if(posts==null){
                channel.sendMessage("Invalid or private subreddit").queue();
                return;
            }

            if(posts.size()==0){
                channel.sendMessage("No posts available").queue();
                return;
            }

            subreddits.put(subreddit, posts);
        }

        sendPost(channel, eb, subreddit);

        System.out.println("Done");
    }

    public ArrayList<Post> getPosts(String subreddit){
        return searchReddit.getPosts(subreddit);
    }

}
