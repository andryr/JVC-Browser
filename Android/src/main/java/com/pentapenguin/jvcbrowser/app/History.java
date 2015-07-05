package com.pentapenguin.jvcbrowser.app;

import com.pentapenguin.jvcbrowser.entities.Topic;

import java.util.ArrayList;

public class History {

    private static ArrayList<Topic> topics = new ArrayList<Topic>();

    public static void add(Topic topic) {
        if (!topics.contains(topic)) topics.add(topic);
    }

    public static void remove(Topic topic) {
        topics.remove(topic);
    }

    public static ArrayList<Topic> getList() {
        return topics;
    }

    public static void reset() {
        topics.clear();
    }
}
