package Reddit;

/**
 * Stores information about comments from the subreddit post.
 */
public class RedditComment {
    private int score;
    private String author, body;

    public RedditComment(int score, String author, String body){
        this.score=score;
        this.author=author;
        this.body=body;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
