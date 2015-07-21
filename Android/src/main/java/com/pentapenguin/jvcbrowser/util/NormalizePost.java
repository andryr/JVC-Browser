package com.pentapenguin.jvcbrowser.util;

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

        for (Element noelshack : noelshacks) {
            Element img = noelshack.child(0);
            String src = "http:" + img.attr("src");

            img.attr("src", src);

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
            spoil.append("<span class=\"barre-head\">\n" +
                    "<span class=\"txt-spoil\">Spoil</span>\n" +
                    "</span>");
        }
    }

    private String minifyLink(String url) {
        int max = 40;

        if (url.length() > max) {
            url = url.substring(0, max / 2) + "[...]" + url.substring(url.length() - max/2, url.length());
        }

        return url;
    }

    private static String html(String input) {
        return "<html><head><link href=\"css/forum-mobile.css\" rel=\"stylesheet\">" +
                "</head><body><div class=\"contenu\">" + input + "</div></body></html>";
    }

    public Element getContent() {
        return content;
    }
}
