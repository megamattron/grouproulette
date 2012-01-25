package controllers;

import play.*;
import play.modules.redis.Redis;
import play.mvc.*;

import java.util.*;

import models.*;
import services.Data;

public class Application extends Controller {

    public static void index() {
        String value = Redis.get("blam");
        Data.main(null);
        render(value);
    }

}