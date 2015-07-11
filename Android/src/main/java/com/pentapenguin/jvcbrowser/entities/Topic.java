package com.pentapenguin.jvcbrowser.entities;

import android.os.Parcel;
import android.os.Parcelable;

public class Topic extends Item implements Parcelable {

    private String author;
    private String lastPostDate;
    private String thumbUrl;
    private int postsNumber;
    private int code = 42;
    private int idForum;
    private int page = 1;

    public Topic(int id, String content) {
        super(id, content);
    }

    public Topic(int id, int code, int idForum, int page) {
        this(id, code, idForum);
        this.page = page;
    }

    public Topic(int id, int code, int idForum) {
        this.id = id;
        this.idForum = idForum;
        this.code = code;
    }

    public Topic(int id, int code, int idForum, String content) {
        this.id = id;
        this.idForum = idForum;
        this.code = code;
        this.content = content;
    }

    public Topic(int id, int code, int idForum, String content, String author, String lastPostDate, String thumbUrl, int postsNumber) {
        super(id, content);
        this.code = code;
        this.idForum = idForum;
        this.author = author;
        this.lastPostDate = lastPostDate;
        this.thumbUrl = thumbUrl;
        this.postsNumber = postsNumber;
    }

    public Topic(int id, int code, int idForum, String content, int page, String author, String lastPostDate, String thumbUrl, int postsNumber) {
        super(id, content);
        this.code = code;
        this.idForum = idForum;
        this.author = author;
        this.lastPostDate = lastPostDate;
        this.thumbUrl = thumbUrl;
        this.postsNumber = postsNumber;
        this.page = page;
    }

    public Topic(Parcel parcel) {
        id = parcel.readInt();
        code = parcel.readInt();
        idForum = parcel.readInt();
        content = parcel.readString();
        page = parcel.readInt();
        author = parcel.readString();
        lastPostDate = parcel.readString();
        thumbUrl = parcel.readString();
        postsNumber = parcel.readInt();
    }

    public Topic page(int page) {
        if (page > 0) {
            return new Topic(id, code, idForum, page);
        }
        return this;
    }

    public String getAuthor() {
        return author;
    }

    public String getLastPostDate() {
        return lastPostDate;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public int getPage() {
        return page;
    }

    public int getPostsNumber() {
        return postsNumber;
    }

    public int getCode() {
        return code;
    }

    public int getIdForum() {
        return idForum;
    }

    public static final Parcelable.Creator<Topic> CREATOR = new Parcelable.Creator<Topic>() {
        @Override
        public Topic createFromParcel(Parcel source) {
            return new Topic(source);
        }

        @Override
        public Topic[] newArray(int size) {
            return new Topic[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(code);
        parcel.writeInt(idForum);
        parcel.writeString(content);
        parcel.writeInt(page);
        parcel.writeString(author);
        parcel.writeString(lastPostDate);
        parcel.writeString(thumbUrl);
        parcel.writeInt(postsNumber);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Topic && ((Topic) o).getId() == id;
    }

    @Override
    public String toString() {
        return super.toString() + ", idForum: " + idForum + ", code: " + code + ", page: " + page;
    }
}
