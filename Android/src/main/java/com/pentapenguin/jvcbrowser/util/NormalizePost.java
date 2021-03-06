package com.pentapenguin.jvcbrowser.util;

import com.pentapenguin.jvcbrowser.app.Assets;
import com.pentapenguin.jvcbrowser.app.Settings;
import com.pentapenguin.jvcbrowser.util.persistence.Storage;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

public class NormalizePost {

    private Element content;

    public NormalizePost(Element content) {
        this.content = content;
    }

    public static NormalizePost parse(Element content) {
        return new NormalizePost(content);
    }

    public String text() {
        smileys();
        noelshack();
        iframe();
        link();
        spoil();
        return html(content.toString());
    }

    private void smileys() {
        Elements smileys = content.getElementsByAttributeValue("data-def", "SMILEYS");

        for (Element smiley : smileys) {
            String src = "http:" + smiley.attr("src");
            smiley.attr("src", src);
        }
    }

    private void noelshack() {
        Elements noelshacks = content.getElementsByAttributeValue("data-def", "NOELSHACK");
        int i = 0;
        int max = 20;

        for (Element noelshack : noelshacks) {
            i++;
            Element img = noelshack.child(0);
            String src = "http:" + img.attr("src");

            img.attr("src", src);
//            if (i > max) {
//                Attributes attrs = new Attributes();
//                attrs.put("href", src);
//                img.replaceWith(new Element(Tag.valueOf("a"), "", attrs).text(minifyLink(src)));
//            }
        }
    }

    private void iframe() {
        Elements iframes = content.getElementsByTag("iframe");

        for (Element iframe : iframes) {
            String url = "http:" + iframe.attr("src");

            iframe.replaceWith(new Element(Tag.valueOf("a"), "").attr("href", url).text(minifyLink(url)));
        }
    }

    private void link() {
        Elements links = content.getElementsByClass("JvCare");

        for (Element link : links) {
            String src;
            if (link.hasAttr("title")) {
                src = link.attr("title");
            } else
                src = link.text();
            link.replaceWith(new Element(Tag.valueOf("a"), "").attr("href", src).text(minifyLink(src)));
        }
    }

    private void spoil() {
        Elements spoils = content.getElementsByClass("bloc-spoil-jv");

        for (Element spoil : spoils) {
            spoil.prepend("<br /><span class=\"barre-head\">\n" +
                    "<span class=\"txt-spoil\">Spoil</span>\n" +
                    "</span>")
                    .append("<span class=\"barre-head\">\n" +
                            "<span class=\"txt-spoil\">Spoil</span>\n" +
                            "</span><br />");
        }
    }

    private String minifyLink(String url) {
        int max = 30;

        if (url.length() > max) {
            url = url.substring(0, max / 2) + "[...]" + url.substring(url.length() - max/2, url.length());
        }

        return url;
    }

    private static String html(String input) {
        String css = Assets.css[Storage.getInstance().get(Settings.THEME, 1)];
        return "<html><head><link href=\"" + css + "\" rel=\"stylesheet\">" +
                "</head><body><div class=\"contenu\">" + input + "</div></body></html>";
    }

    public Element getContent() {
        return content;
    }
}
