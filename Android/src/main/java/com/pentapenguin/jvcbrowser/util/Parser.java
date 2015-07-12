package com.pentapenguin.jvcbrowser.util;

import com.pentapenguin.jvcbrowser.app.App;
import com.pentapenguin.jvcbrowser.entities.*;
import com.pentapenguin.jvcbrowser.exceptions.NoContentFoundException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    // Divers

    public static HashMap<String, String> hidden(Document doc, String classForm) {
        HashMap<String, String> map = new HashMap<String, String>();
        Element form;

        if (doc.getElementsByClass(classForm).size() == 0)
            return map;
        form = doc.getElementsByClass(classForm).get(0);
        Elements hiddens = form.getElementsByTag("input");
        for (Element hidden : hiddens) {
            if (hidden.attr("type").equals("hidden"))
                map.put(hidden.attr("name"), hidden.attr("value"));
        }

        return map;
    }

    public static String captcha(Document doc, String classCaptcha) {
        Elements content = doc.getElementsByClass(classCaptcha);

        if (content.isEmpty()) return null;
        Elements imgs = content.get(0).getElementsByTag("img");
        if (imgs.isEmpty()) return null;

        return App.HOST_MOBILE + imgs.get(0).attr("src");
    }

    public static String error(Document doc, String classError) {
        Elements content = doc.getElementsByClass(classError);

        if (content.isEmpty()) {
            return null;
        }

        return content.get(0).text();
    }

    // Forum

    // Topic

    public static ArrayList<Link> listFavorite(Document doc, int what) throws NoContentFoundException {
        ArrayList<Link> links = new ArrayList<Link>();
        Elements content = doc.getElementsByClass("liste-with-chevron");

        if (content.isEmpty()) {
            throw new NoContentFoundException();
        }
        Elements favs = content.get(what).getElementsByTag("a");

        if (favs.isEmpty()) {
            throw new NoContentFoundException();
        }
        for (Element forum : favs) {
            String url = "http:" + forum.attr("href");
            String text = forum.text();

            links.add(new Link(url, text));
        }

        return links;
    }

    public static ArrayList<Topic> forum(Document doc) throws NoContentFoundException {
        ArrayList<Topic> topics = new ArrayList<Topic>();
        Elements content = doc.getElementsByClass("liste-topics");

        if (content.isEmpty())
            throw new NoContentFoundException();

        Elements lis = content.get(0).getElementsByTag("li");
        if (lis.isEmpty())
            throw new NoContentFoundException();

        for (Element li : lis) {
            String url = App.HOST_MOBILE + li.getElementsByTag("a").get(0).attr("href");
            String title = li.getElementsByClass("titre-topic").get(0).text();
            String name = "";
            int posts = 0, id = 0, idForum = 0, code = 0;
            String author = li.getElementsByClass("auteur").get(0).text();
            String thumbUrl = App.HOST_MOBILE + li.getElementsByTag("img").get(0).attr("src");
            String date = li.getElementsByClass("date-post-topic").get(0).text();
            Matcher matcher = Pattern.compile("^(.*) ?\\(([0-9]+)\\)$").matcher(title);

            if (matcher.find()) {
                name = matcher.group(1);
                posts = Integer.parseInt(matcher.group(2));

            }
            matcher = Pattern.compile("forums/([0-9]+)-([0-9]+)-([0-9]+)-").matcher(url);
            if(matcher.find()) {
                code = Integer.parseInt(matcher.group(1));
                idForum = Integer.parseInt(matcher.group(2));
                id = Integer.parseInt(matcher.group(3));

            }
            topics.add(new Topic(id, code, idForum, name, author, date, thumbUrl, posts));
        }
        return topics;
    }

    public static Topic newTopic(Document doc) throws NoContentFoundException {
        Elements div = doc.getElementsByClass("bloc-consultation");

        if (div.isEmpty()) {
            throw new NoContentFoundException();
        }

        Elements a = div.get(0).getElementsByTag("a");
        if (a.isEmpty()) {
            throw new NoContentFoundException();
        }

        return (Topic) Helper.urlResolve(a.get(0).attr("href"));
    }

    public static ArrayList<Post> topic(Document doc) throws NoContentFoundException {
        ArrayList<Post> posts = new ArrayList<Post>();
        Elements content = doc.getElementsByClass("liste-messages");
        if (content.isEmpty()) {
            throw new NoContentFoundException();
        }

        Elements divs = content.get(0).getElementsByClass("post");
        if (divs.isEmpty())
            throw new NoContentFoundException();

        for (Element div : divs) {
            Matcher matcher  = Pattern.compile("post_(\\d*)").matcher(div.attr("id"));
            int id = 0;
            String author, thumb = null, thumbUrl, date;
            Element message;
            if (matcher.find()) {
                id = Integer.parseInt(matcher.group(1));
            }
            Elements avatar = div.getElementsByClass("user-avatar-msg");
            if (!avatar.isEmpty())
                thumb = "http:" + avatar.get(0).attr("src");
            Element a = div.getElementsByClass("bloc-pseudo-msg").get(0);
            author = a.text();
            thumbUrl = "http:" + a.attr("href");
            date = div.getElementsByClass("date-post").get(0).text();
            Element msg = div.getElementsByClass("contenu").get(0);
            msg.getElementsByClass("lire-suite-msg").remove();
            message = div.getElementsByClass("contenu").get(0);
            posts.add(new Post(id, NormalizePost.parse(message).text(), message, author, date, thumb, thumbUrl));
        }

        return posts;
    }

    public static int page(Document doc) throws NoContentFoundException {
        Elements content = doc.getElementsByClass("pagination-b");
        List<Integer> pages = new ArrayList<Integer>();
        pages.add(1);
        if (content.isEmpty())
            throw new NoContentFoundException();

        Elements as = content.get(0).getElementsByTag("a");
        for (Element a : as) {
            String page = a.text();
            if (!page.contains("<") && !page.contains(">"))
                pages.add(Integer.parseInt(a.text()));
        }
        Elements span = doc.getElementsByClass("num-page");
        if (!span.isEmpty()) pages.add(Integer.parseInt(span.get(0).text()));

        return Collections.max(pages);
    }

    public static String timestamp(String html) {
        String name = "ajax_timestamp_preference_user";
        String pattern = "<input type=\"hidden\" name=\"" + name+ "\" " +
                "id=\"" + name + "\" value=\"([0-9]*?)\" />";
        Matcher matcher = Pattern.compile(pattern).matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    public static String hash(String html, String name) {
        String pattern = "<input type=\"hidden\" name=\"" + name +"\" " +

                "id=\""+ name +"\" value=\"(\\w*?)\" />";
        Matcher matcher = Pattern.compile(pattern).matcher(html);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }

    public static String hash1(String html) {
        return hash(html, "ajax_hash_liste_messages");
    }

    public static String hash2(String html) {
        return hash(html, "ajax_hash_moderation_forum");
    }

    public static String hash3(String html) {
        return hash(html, "ajax_hash_preference_user");
    }

    public static int idTopic(String html) {
        Matcher matcher = Pattern.compile("var id_topic = ([0-9]*);").matcher(html);
        if (matcher.find()) return Integer.parseInt(matcher.group(1));

        return -1;
    }

    public static String textArea(Document doc) {
        Elements areas = doc.getElementsByTag("textarea");

        if (!areas.isEmpty()) {
            return areas.get(0).text();
        }
        return "";
    }

    public static String getTitleTopic(Document doc) {
        Elements title = doc.getElementsByClass("bloc-nom-sujet");

        if (!title.isEmpty()) {
            return title.get(0).getElementsByTag("span").get(0).text();
        }
        return "";
    }

    public static String getTitleForum(Document doc) {
        String title = "";
        Elements divs = doc.getElementsByClass("bloc-languette");

        if (!divs.isEmpty()) {
            for (Element div : divs) {
                Elements etage = div.getElementsByClass("etage-2");
                title += etage.get(0).text() + " : ";
            }
        }

        return title.substring(0, title.length() - 3);
    }

    public static String getTitleMp(Document doc) {
        return doc.getElementById("discussion").getElementsByTag("h1").get(0).text();
    }

    public static ArrayList<Mp> inbox(Document doc) throws NoContentFoundException {
        Elements trs = doc.getElementsByClass("list-msg").get(0).getElementsByTag("tr");

        if (trs.isEmpty()) {
            throw new NoContentFoundException();
        }
        ArrayList<Mp> mps = new ArrayList<Mp>();
        for (int i = 1; i < trs.size(); i++) {
            Element tr = trs.get(i);
            int lu = tr.hasAttr("class") ? 1 : 0;
            Elements tds = tr.getElementsByTag("td");
            String author = tds.get(0).getElementsByTag("span").get(0).text();
            String url = App.HOST_WEB + tds.get(1).getElementsByTag("a").attr("href");
            String content = tds.get(1).getElementsByTag("a").text();
            String date = tds.get(2).text();
            int locked = tr.getElementsByTag("button").size();
            Matcher matcher = Pattern.compile("messages-prives/message\\.php\\?id=([0-9]*?)&").matcher(url);
            int id = 0;
            if(matcher.find()) {
                id = Integer.parseInt(matcher.group(1));
            }
            mps.add(new Mp(id, lu, locked, content, author, date, url, 0));
        }

        return mps;
    }

    public static ArrayList<Post> mp(Document doc) throws NoContentFoundException {
        Elements divs = doc.getElementsByClass("bloc-message-forum");
        ArrayList<Post> posts = new ArrayList<Post>();

        if (divs.isEmpty()) {
            throw new NoContentFoundException();
        }

        for (Element div : divs) {
            String thumb = "http:" + div.getElementsByClass("user-avatar-msg").get(0).attr("src");
            Element a = div.getElementsByClass("bloc-pseudo-msg").get(0);
            String profilUrl = "http:" + a.attr("href");
            String author = a.text();
            String date = div.getElementsByClass("bloc-date-msg").get(0).text();
            Element content = div.getElementsByClass("bloc-contenu").get(0);

            posts.add(new Post(-1, content.html(), content, author, date, thumb, profilUrl));
        }

        return posts;
    }

    public static int mpOffset(Document doc) {
        Elements button = doc.getElementsByClass("btn-25-msg");

        if (button.isEmpty()) {
            return 1;
        }
        Matcher matcher = Pattern.compile("/messages-prives/message\\.php\\?id=[0-9]*?&offset=([0-9]*?)&")
                .matcher(button.get(0).attr("href"));
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 1;
    }

    public static HashMap<String, ArrayList<Forum>> listForums(Document doc) throws NoContentFoundException {
        HashMap<String, ArrayList<Forum>> forums = new HashMap<String, ArrayList<Forum>>();
        Elements sections = doc.getElementsByClass("forum-section");

        if (sections.isEmpty()) throw new NoContentFoundException();

        for (Element section : sections) {
            String name = section.getElementsByTag("h2").get(0).text();
            ArrayList<Forum> fs = new ArrayList<Forum>();
            Elements as = section.getElementsByTag("a");

            for (Element a : as) {
                Forum f = (Forum) Helper.urlResolve(a.attr("href"));
                if (f != null) fs.add(new Forum(f.getId(), a.text()));
            }
            forums.put(name, fs);
        }

        return forums;
    }

    public static Mp newMP(Document doc) throws NoContentFoundException {
        Elements refreshs = doc.getElementsByClass("btn-actualiser");

        if (refreshs.isEmpty()) {
            throw new NoContentFoundException();
        }
        String url = refreshs.get(0).attr("href");
        Matcher matcher = Pattern.compile("message\\.php\\?id=([0-9]*?)&").matcher(url);
        if (matcher.find()) {
            int id = Integer.parseInt(matcher.group(1));
            return new Mp(id, null);
        }

        return null;
    }

    public static int mpNumber(Document doc) {
        Elements nb = doc.getElementsByClass("nb-pages");

        if (!nb.isEmpty()) {
            String number = nb.get(0).text();
            Matcher matcher = Pattern.compile("Messages [0-9-]*? sur ([0-9]*)").matcher(nb.get(0).text());

            if (matcher.find()) return Integer.parseInt(matcher.group(1));
        }

        return 0;
    }

    public static String profilThumb(Document doc) throws NoContentFoundException {
        Elements content = doc.getElementsByClass("content-img-avatar");

        if (content.isEmpty()) throw new NoContentFoundException();

        Element profil = content.get(0).getElementsByTag("img").get(0);

        return "http:" + profil.attr("src");
    }

    public static String profilBackground(Document doc) {
        Element content = doc.getElementById("content");

        if (content.hasAttr("style")) {
            String style = content.attr("style");

            Matcher matcher = Pattern.compile("background: url\\('(.*)'\\) center top no-repeat;").matcher(style);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    public static ArrayList<Forum> searchForum(Document doc) {
        ArrayList<Forum> forums = new ArrayList<Forum>();
        Element block1 = doc.getElementsByClass("bloc1").get(0);
        Elements lis = block1.getElementsByTag("li");

        for (Element li : lis) {
            String url = li.getElementsByTag("a").get(0).attr("href");
            String content = li.getElementsByTag("h2").get(0).text();
            int id = 0;

            Matcher matcher = Pattern.compile("/forums/[0-9]*?-([0-9]*?)-0-1-0-").matcher(url);
            if (matcher.find()) id = Integer.parseInt(matcher.group(1));

            forums.add(new Forum(id, content));
        }

        return forums;
    }

    public static String newPostUrl(Document doc) {
        return App.HOST_MOBILE + doc.getElementsByClass("a-menu-fofo").get(0).attr("href");
    }

    public static String profileContent(Document doc) throws NoContentFoundException {
        Element content = doc.getElementById("content-fmobile");

        if (content == null) throw new NoContentFoundException();

        content.getElementsByClass("footer").remove();
        content.getElementsByClass("bloc-retour-haut").remove();
        Elements images = content.getElementsByTag("img");

        for (Element image : images) {
            String src = image.attr("src");
            if (!src.startsWith("http:")) image.attr("src", "http:" + src);
        }

        return "<html><head><link href=\"css/forum-mobile.css\" rel=\"stylesheet\">" +
                "</head><body>" + content.html() + "</body></html>";
    }
}
