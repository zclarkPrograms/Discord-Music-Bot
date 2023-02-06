package Reddit;
/**
 * Used to store information about the subreddit post.
 */
public class Post {
    private String id, url, subreddit, title, author, permaUrl;

    public Post(String id, String url, String subreddit, String title, String author, String permaUrl) {
        this.id=id;
        this.url = url;
        this.subreddit = subreddit;
        this.title = title;
        this.author = author;
        this.permaUrl = permaUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getPermaUrl() {
        return permaUrl;
    }

    public void setPermaUrl(String permaUrl) {
        this.permaUrl = permaUrl;
    }
}
