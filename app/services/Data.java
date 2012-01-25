package services;

import models.Message;
import models.User;
import play.modules.redis.Redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Manages the data connection with Redis.
 *
 * Set:  Users
 * Set:  Groups (we will use random IDs)
 * List: Messages
 *
 * @author John Watkinson
 */
public class Data {

    private static final String INCR_GROUP = "group_id";
    private static final String INCR_MESSAGE = "message_id";

    private static final String USER = "user";
    private static final String GROUP = "group";
    private static final String MESSAGE = "message";
    private static final String TEXT = "text";

    private static final String SET_USERS = "users";
    private static final String SET_GROUPS = "groups";
    private static final String SET_MEMBERS = "members";

    private static final String LIST_MESSAGES = "messages";

    public static void addUser(User user) {
        Redis.set(USER + ":" + user.id, user.name);
        Redis.sadd(SET_USERS, user.id);
    }

    public static User getUser(String id) {
        String name = Redis.get(USER + ":" + id);
        return new User(id, name);
    }

    public static String addGroup() {
        String id = "" + Redis.incr(INCR_GROUP);
        Redis.sadd(SET_GROUPS, "" + id);
        return id;
    }

    public static Set<String> getAllGroups() {
        return Redis.smembers(SET_GROUPS);
    }
    
    public static void addUserToGroup(String userId, String groupId) {
        Redis.sadd(GROUP + ":" + groupId + ":" + SET_MEMBERS, userId);
    }

    public static void removeUserFromGroup(String userId, String groupId) {
        Redis.srem(GROUP + ":" + groupId + ":" + SET_MEMBERS, userId);
    }
    
    public static Set<String> getUsersForGroup(String groupId) {
        return Redis.smembers(GROUP + ":" + groupId + ":" + SET_MEMBERS);
    }
    
    public static String postMessage(String userId, String groupId, String text) {
        String id = "" + Redis.incr(INCR_MESSAGE);
        Redis.set(MESSAGE + ":" + id + ":" + GROUP, groupId);
        Redis.set(MESSAGE + ":" + id + ":" + USER, userId);
        Redis.set(MESSAGE + ":" + id + ":" + TEXT, text);
        Redis.rpush(GROUP + ":" + groupId + ":" + LIST_MESSAGES, id);
        return id;
    }

    public static List<Message> getMessagesForGroup(String groupId) {
        final List<String> list = Redis.lrange(GROUP + ":" + groupId + ":" + LIST_MESSAGES, 0, -1);
        ArrayList<Message> result = new ArrayList<Message>();
        for (String key : list) {
            String userId = Redis.get(MESSAGE + ":" + key + ":" + USER);
            String text = Redis.get(MESSAGE + ":" + key + ":" + TEXT);
            Message m = new Message(userId, text);
            result.add(m);
        }
        return result;
    }

    public static void main(String[] args) {
        User user1 = new User("user1", "Billy Blogpost");
        User user2 = new User("user1", "Jimmy Donut");
        addUser(user1);
        addUser(user2);
        String groupId = addGroup();
        addUserToGroup(user1.id, groupId);
        addUserToGroup(user2.id, groupId);
        postMessage(user1.id, groupId, "Billy Blogpost here with another weblog entry, thanks.");
        postMessage(user2.id, groupId, "Couldn't be happier over here, all the best, Jimmy Donut.");
        final Set<String> allGroups = getAllGroups();
        for (String group : allGroups) {
            System.out.println("Group: " + group);
        }
        final List<Message> messagesForGroup = getMessagesForGroup(groupId);
        for (Message message : messagesForGroup) {
            System.out.println("Message: " + message);
        }
    }
}
