package com.pentapenguin.jvcbrowser.util;

import android.util.TypedValue;
import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.entities.Forum;
import com.pentapenguin.jvcbrowser.entities.Item;
import com.pentapenguin.jvcbrowser.entities.Topic;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {

    public static String forumToRaw(Forum forum) {
        String page = Integer.toString(1 + (forum.getPage() - 1)*25);
        String id = Integer.toString(forum.getId());
        return "/forums/0-" + id + "-0-1-0-" + page + "-0-0.htm";
    }

    public static String forumToMobileUrl(Forum forum) {
        return App.HOST_MOBILE + forumToRaw(forum);
    }

    public static String forumToUrl(Forum forum) {
        return App.HOST_WEB + forumToRaw(forum);
    }

    public static String forumToApiUrl(Forum forum) {
        return App.HOST_API + forumToRaw(forum);
    }

    public static String forumToSearchRaw(Forum forum, int choice, String what) {
        String mode = choice == 0 ? "titre_topic" : "auteur_topic";
        return "/recherche" + forumToRaw(forum) + "?" +
                "type_search_in_forum=" + mode +"&search_in_forum=" + what.replace(" ", "+");
    }

    public static String forumToSearchMobileUrl(Forum forum, int choice, String what) {
        return App.HOST_MOBILE + forumToSearchRaw(forum, choice, what);
    }

    public static String forumToSearchApiUrl(Forum forum, int choice, String what) {
        return App.HOST_API + forumToSearchRaw(forum, choice, what);
    }

    public static String topicToRaw(Topic topic) {
        String code = Integer.toString(topic.getCode());
        String idForum = Integer.toString(topic.getIdForum());
        String id = Integer.toString(topic.getId());
        String page = Integer.toString(topic.getPage());

        return "/forums/" + code + "-" + idForum + "-" + id + "-" + page + "-0-1-0-0.htm";
    }

    public static String topicToMobileUrl(Topic topic) {
        return App.HOST_MOBILE + topicToRaw(topic);
    }

    public static String topicToUrl(Topic topic) {
        return App.HOST_WEB + topicToRaw(topic);
    }

    public static String topicToApiUrl(Topic topic) {
        return App.HOST_API + topicToRaw(topic);
    }

    public static Item urlResolve(String url) {
        Matcher matcher = Pattern.compile("forums/([0-9]*)-([0-9]*)-([0-9]*)-([0-9]*)-0-([0-9]*)-").matcher(url);

        if (matcher.find()) {
            int code = Integer.parseInt(matcher.group(1));
            int idForum = Integer.parseInt(matcher.group(2));
            int idTopic = Integer.parseInt(matcher.group(3));
            int pageTopic = Integer.parseInt(matcher.group(4));
            int pageForum = Integer.parseInt(matcher.group(5));

            if (code == 0) {
                return new Forum(idForum, pageForum, "");
            } else {
                return new Topic(idTopic, code, idForum, pageTopic);
            }
        }
        return null;
    }

    public static String mpToUrl(Topic topic, int offset) {
        return App.HOST_WEB + "/messages-prives/message.php?id=" + topic.getId() + (offset != 0 ? "&offset=" + offset : "") ;
    }

    public static String inboxToUrl(int page) {
        return "http://www.jeuxvideo.com/messages-prives/boite-reception.php?p=" + page;
    }

    public static int dip2Pixel(int dipValue){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue,
                App.getContext().getResources().getDisplayMetrics());
    }
}
