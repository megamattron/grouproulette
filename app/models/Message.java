package models;

/**
 * @author John Watkinson
 */
public class Message {
    
    public String id;
    public String userId;
    public String text;

    public Message(String id, String userId, String text) {
        this.userId = userId;
        this.text = text;
    }

    @Override
    public String toString() {
        return "(" + id + ") [" + userId + "]: " + text;
    }
}
