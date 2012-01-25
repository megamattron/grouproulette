package controllers;

import play.*;
import play.modules.redis.Redis;
import play.mvc.*;

public class Application extends Controller {

    public static void index() {
        // todo - render login page if no cookie, main page if there is a cookie
        String value = Redis.get("blam");
        render(value);
    }

    public static void login(String username) {
        // todo - store some stuff, render main page, store cookie
        String groupId = "the group";
        render(username, groupId);
    }

    public static void getMessages(String groupId) {
        // todo - get all messages, dump to json
        renderJSON(null);
    }
    
    public static void postMessage(String groupId, String text) {
        // todo - get username from cookie (or should it be a request param?), store
        renderJSON(null);
    }
    
    public static void poll(String groupId, String lastMessageId) {
        // todo - get the group messages and see if there is anything after the message ID. If so, then JSON it.
        renderJSON(null);
    }
}