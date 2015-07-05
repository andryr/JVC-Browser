package com.pentapenguin.jvcbrowser.entities;

public abstract class Item {

    protected int id;
    protected String content;

    protected Item() { }

    protected Item(int id, String content) {
        this.id = id;
        this.content = content;
    }

    public int getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "id: " + id;
    }
}
