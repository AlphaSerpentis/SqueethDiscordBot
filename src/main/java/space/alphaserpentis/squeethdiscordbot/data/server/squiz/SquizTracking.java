package space.alphaserpentis.squeethdiscordbot.data.server.squiz;

import java.util.ArrayList;
import java.util.HashMap;

public class SquizTracking {
    public static class UserData {
        public static class Response {
            /**
             * Response time in milliseconds between question being issued and user responding to it
             */
            public long responseTime;
            /**
             * String representing the date of when the response was made
             */
            public String date;
        }

        public ArrayList<Response> responses;
    }

    public HashMap<Long, HashMap<Long, UserData>> serverMapping;
}