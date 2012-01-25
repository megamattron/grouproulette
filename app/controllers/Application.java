package controllers;

import models.Message;
import models.User;
import play.*;
import play.modules.redis.Redis;
import play.mvc.*;
import services.Data;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Application extends Controller {

    private static final String USERNAME = "username";

    public static void index() {
        // todo - render login page if no cookie, main page if there is a cookie
        String username = getUsername();
        if (username == null) {

        } else {
            Logger.info("User has already logged in as " + username);
        }
        String value = Redis.get("blam");
        render(value);
    }

    private static String getUsername() {
        return session.get(USERNAME);
    }

    public static void login(String username) {
        session.put(USERNAME, username);
        Logger.info("You are now user " + username);
        String groupId = "the group";

        Data.addUserToGroup(username, groupId);
        render(username, groupId);
    }

    public static void getUsers(String groupId) {
//        List<User> usersForGroup = Data.getUsersForGroup(groupId);
//        renderJSON(usersForGroup);
    }

    public static void getMessages(String groupId) {
        List<Message> messagesForGroup = Data.getMessagesForGroup(groupId);
        renderJSON(messagesForGroup);
    }

    public static void postMessage(String groupId, String text) {
        // todo - get username from cookie (or should it be a request param?), store
        renderJSON(null);
    }
    
    public static void poll(String groupId, String lastMessageId) {
        final List<Message> messages = Data.getMessagesForGroup(groupId);
        final Iterator<Message> iterator = messages.iterator();
        long lastID = Long.parseLong(lastMessageId);
        // Remove all the ones we already saw
        while (iterator.hasNext()) {
            Message message = iterator.next();
            long id = Long.parseLong(message.id);
            if (id <= lastID) {
                iterator.remove();
            }
        }
        renderJSON(messages);
    }

    public static void makeTestData() {
        User user1 = new User("user1", "Billy Blogpost");
        User user2 = new User("user1", "Jimmy Donut");
        Data.addUser(user1);
        Data.addUser(user2);
        String groupId = Data.addGroup();
        Data.addUserToGroup(user1.id, groupId);
        Data.addUserToGroup(user2.id, groupId);
        Data.postMessage(user1.id, groupId, "Billy Blogpost here with another weblog entry, thanks.");
        Data.postMessage(user2.id, groupId, "Couldn't be happier over here, all the best, Jimmy Donut.");
        final Set<String> allGroups = Data.getAllGroups();
        for (String group : allGroups) {
            System.out.println("Group: " + group);
        }
        final List<Message> messagesForGroup = Data.getMessagesForGroup(groupId);
        for (Message message : messagesForGroup) {
            System.out.println("Message: " + message);
        }
        System.out.println("Group for Billy: " + Data.getGroupForUser(user1.id));
    }
}