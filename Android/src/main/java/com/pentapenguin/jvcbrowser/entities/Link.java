package com.pentapenguin.jvcbrowser.entities;

public class Link {

    private String url;
    private String content;

    public Link(String url, String content) {
        this.url = url;
        this.content = content;
    }

    public String getUrl() {
        return url;
    }

    public String getContent() {
        return content;
    }
}
