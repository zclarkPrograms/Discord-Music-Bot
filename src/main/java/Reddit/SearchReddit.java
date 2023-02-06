package Reddit;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.NetworkException;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.*;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.pagination.DefaultPaginator;
import net.dean.jraw.references.SubredditReference;

import java.util.ArrayList;

public class SearchReddit {
    private String username, password, clientID, clientSecret;

    public SearchReddit(String username, String password, String clientID, String clientSecret){
        this.username = username;
        this.password = password;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
    }

    public ArrayList<Post> getPosts(String subreddit){
        RedditClient redditClient = getRedditClient();

        ArrayList<Post> posts=new ArrayList<>();

        try{
            SubredditReference ref=redditClient.subreddit(subreddit);

            //makes sure that subreddit is sfw before collecting posts
            if(!ref.about().isNsfw()) {
                //gets today's highest-rated posts from a given subreddit
                DefaultPaginator<Submission> paginator = ref
                        .posts()
                        .sorting(SubredditSort.TOP)
                        .timePeriod(TimePeriod.DAY)
                        .build();

                //collects and stores information from each post
                Listing<Submission> submissions = paginator.next();
                for (Submission s : submissions) {
                    //creates a link to the actual post
                    String permalink = "https://reddit.com" + s.getPermalink();

                    Post post = new Post(s.getId(), s.getUrl(), s.getSubreddit(), s.getTitle(), s.getAuthor(), permalink);

                    //makes sure posts are sfw and contain either a .jpg or .png file, so we don't have to handle videos of gifs.
                    if (!s.isNsfw() && (s.getUrl().contains(".jpg") || s.getUrl().contains(".png"))) {
                        posts.add(post);
                    }
                }
            }

            System.out.println("Reddit search done!");
        }
        //invalid or private subreddit
        catch(NetworkException | ApiException | NullPointerException ex){
            return null;
        }

        return posts;
    }

    /**
     * gets reddit client to use for post retrieval, passing in account and application credentials.
     */
    public RedditClient getRedditClient(){
        UserAgent userAgent = new UserAgent("Chrome", "com.discord.bot",
                "v1.0", "SlothsAllTheWay");
        Credentials credentials= Credentials.script(username, password,
                clientID, clientSecret);
        NetworkAdapter networkAdapter = new OkHttpNetworkAdapter(userAgent);

        return OAuthHelper.automatic(networkAdapter, credentials);
    }

}
