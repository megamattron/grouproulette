package controllers;

import models.Message;
import models.User;
import play.*;
import play.modules.redis.Redis;
import play.mvc.*;
import services.Data;
import util.AeSimpleSHA1;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Application extends Controller {

    private static final String USERNAME = "username";

    public static void index() {
        // todo - render login page if no cookie, main page if there is a cookie
        String username = getUsername();
        if (username == null) {
            render();
        } else {
            Logger.info("User has already logged in as " + username);
            String groupForUser = Data.getGroupForUser(username);
            if (groupForUser == null) {
                login(username);
            } else {
                render("Application/group.html", username, groupForUser);
            }
        }
    }

    private static class UserInfo {
        String username;
        String groupId;

        private UserInfo(String username, String groupId) {
            this.username = username;
            this.groupId = groupId;
        }
    }

    private static String getUsername() {
        return session.get(USERNAME);
    }

    private static UserInfo getUserInfo() {
        String username = getUsername();
        String groupForUser = Data.getGroupForUser(username);
        return new UserInfo(username, groupForUser);
    }

    public static void login(String username) {
        setUserAndAddToGroup(username);

        group();
    }

    public static void logout() {
        UserInfo username = getUserInfo();
        if (username != null) {
            Data.removeUserFromGroup(username.groupId, username.groupId);
            session.remove(USERNAME);
            Logger.info("User logged out.");
        }
        twitterLogin();
    }

    private static void setUserAndAddToGroup(String username) {
        session.put(USERNAME, username);
        Logger.info("You are now user " + username);
        String groupId = "the group";
        Data.addUserToGroup(username, groupId);
    }

    public static void group() {
        String username = getUsername();
        String groupId = Data.getGroupForUser(username);
        render(username, groupId);
    }

    public static void getUsers() {
        UserInfo info = getUserInfo();
        Set<User> usersForGroup = Data.getUsersForGroup(info.groupId);
        renderJSON(usersForGroup);
    }

    public static void getMessages() {
        UserInfo info = getUserInfo();
        List<Message> messagesForGroup = Data.getMessagesForGroup(info.groupId);
        renderJSON(messagesForGroup);
    }

    public static void postMessage(String text) {
        UserInfo info = getUserInfo();
        String msgId = Data.postMessage(info.username, info.groupId, text);
        renderJSON(msgId);
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

    private static String TWITTER_SECRET = (String)Play.configuration.get("twitter.secret");

    public static void twitterLogin() {
        Http.Cookie twitterId = request.cookies.get("twitter_anywhere_identity");
        try {
            if (twitterId != null) {
                Logger.info("Twitter id: " + twitterId.value);
                String[] tokens = twitterId.value.split(":");
                boolean idMatch = AeSimpleSHA1.SHA1(tokens[0] + TWITTER_SECRET).equals(tokens[1]);
                Logger.info("Match Hex: " + idMatch);
                if (idMatch) {
                    setUserAndAddToGroup(tokens[0]);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            Logger.error(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            Logger.error(e.getMessage(), e);
        }
        String username = getUsername();
        Logger.info("Username: " + username);
        render(username);
    }

    public static void setUserDetails(String name, String profileUrl) {
        String username = getUsername();
        if (username != null) {
            User user = new User(username, name, profileUrl);
            Data.addUser(user);
            Logger.info("User details now: " + user);
            renderText("success");
        } else {
            Logger.info("No user logged in.");
            renderText("failed");
        }
    }

    public static void makeTestData() {
        User user1 = new User("user1", "Billy Blogpost", null);
        User user2 = new User("user2", "Jimmy Donut", null);
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