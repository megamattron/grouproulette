package controllers;

import models.Message;
import models.User;
import play.*;
import play.libs.F;
import play.modules.redis.Redis;
import play.mvc.*;
import services.Data;
import util.AeSimpleSHA1;

import java.util.HashMap;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Application extends Controller {

    private static enum EventType {
        JOIN,
        LEAVE,
        MESSAGE
    }

    private static class Event {
        EventType type;
        String userId;
        String text;

        private Event(EventType type, String userId, String text) {
            this.type = type;
            this.userId = userId;
            this.text = text;
        }

        @Override
        public String toString() {
            switch (type) {
                case JOIN:
                    return "* " + userId + " joined.";
                case LEAVE:
                    return "* " + userId + " left.";
                case MESSAGE:
                    return userId + ": " + text;
                default:
                    return "";
            }
        }
    }

    private static HashMap<String, F.ArchivedEventStream<Event>> streams = new HashMap<String, F.ArchivedEventStream<Event>>();

    private synchronized static F.ArchivedEventStream<Event> getGroupStream(String groupId) {
        F.ArchivedEventStream<Event> stream = streams.get(groupId);
        if (stream == null) {
            stream = new F.ArchivedEventStream<Event>(100);
            streams.put(groupId, stream);
        }
        return stream;
    }

    private static final String USERNAME = "username";

    public static void index() {
        // todo - render login page if no cookie, main page if there is a cookie
        String username = getUsername();
        if (username == null) {
            render();
        } else {
            Logger.info("User has already logged in as " + username);
            String groupId = Data.getGroupForUser(username);
            if (groupId == null) {
                login(username);
            } else {
                render("Application/group.html", username, groupId);
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

    public static void room() {
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
        String msgId = doMessage(text, info);
        renderJSON(msgId);
    }

    private static String doMessage(String text, UserInfo info) {
        String msgId = Data.postMessage(info.username, info.groupId, text);
        Event event = new Event(EventType.MESSAGE, info.username, text);
        getGroupStream(info.groupId).publish(event);
        return msgId;
    }

    public static void poll(String lastMessageId) {
        UserInfo info = getUserInfo();
        final List<Message> messages = Data.getMessagesForGroup(info.groupId);
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

    public static class Socket extends WebSocketController {

        public static void join() {
            String username = getUsername();
            String groupId = Data.getGroupForUser(username);
            final F.EventStream<Event> stream = getGroupStream(groupId).eventStream();
            stream.publish(new Event(EventType.JOIN, username, ""));
            while (inbound.isOpen()) {
                F.Either<Http.WebSocketEvent, Event> e = await(F.Promise.waitEither(
                        inbound.nextEvent(),
                        stream.nextEvent()
                ));
                for (String userMessage : Http.WebSocketEvent.TextFrame.match(e._1)) {
                    System.out.println("Received: '" + userMessage + "'.");
                    doMessage(userMessage, new UserInfo(username, groupId));
                }
                for (Event event : F.Matcher.ClassOf(Event.class).match(e._2)) {
                    outbound.send(event.toString());
                }
                for(Http.WebSocketClose closed: Http.WebSocketEvent.SocketClosed.match(e._1)) {
                    stream.publish(new Event(EventType.LEAVE, username, ""));
                }
            }
        }

    }
}