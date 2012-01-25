package models;

/**
 * @author John Watkinson
 */
public class User {
    
    public String id;
    public String name;
    public String profileImageUrl;

    public User(String id, String name, String profileImageUrl) {
        this.id = id;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                '}';
    }
}
