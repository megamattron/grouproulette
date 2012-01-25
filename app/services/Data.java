package services;

import models.Message;
import models.User;
import play.modules.redis.Redis;

import java.util.ArrayList;
import java.util.HashSet;
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
    private static final String NAME = "name";
    private static final String PROFILEURL = "profile_url";

    private static final String SET_USERS = "users";
    private static final String SET_GROUPS = "groups";
    private static final String SET_MEMBERS = "members";

    private static final String LIST_MESSAGES = "messages";

    private static void delete(String key) {
        Redis.del(new String[]{key});
    }
    
    public static void addUser(User user) {
        Redis.set(USER + ":" + user.id + ":" + NAME, user.name);
        Redis.set(USER + ":" + user.id + ":" + PROFILEURL, user.profileImageUrl);
        Redis.sadd(SET_USERS, user.id);
    }

    public static User getUser(String id) {
        String name = Redis.get(USER + ":" + id + ":" + NAME);
        String profileUrl = Redis.get(USER + ":" + id + ":" + PROFILEURL);
        return new User(id, name, profileUrl);
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
        Redis.set(USER + ":" + userId + ":" + GROUP, groupId);        
    }
    
    public static String getGroupForUser(String userId) {
        return Redis.get(USER + ":" + userId + ":" + GROUP);
    }

    public static void removeUserFromGroup(String userId, String groupId) {
        Redis.srem(GROUP + ":" + groupId + ":" + SET_MEMBERS, userId);
        delete(USER + ":" + userId + ":" + GROUP);
    }
    
    public static Set<User> getUsersForGroup(String groupId) {
        Set<String> userIds = Redis.smembers(GROUP + ":" + groupId + ":" + SET_MEMBERS);
        Set<User> users = new HashSet<User>();
        for (String userId : userIds) {
            users.add(getUser(userId));
        }
        return users;
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
            Message m = new Message(key, userId, text);
            result.add(m);
        }
        return result;
    }

}
