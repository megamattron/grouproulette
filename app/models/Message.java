package models;

/**
 * @author John Watkinson
 */
public class Message {
    
    public String userId;
    public String text;

    public Message(String userId, String text) {
        this.userId = userId;
        this.text = text;
    }

    @Override
    public String toString() {
        return "[" + userId + "]: " + text;
    }
}
