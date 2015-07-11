package com.pentapenguin.jvcbrowser.entities;

import android.os.Parcel;
import android.os.Parcelable;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class Post extends Item implements Parcelable{

    private String author;
    private String date;
    private String profilThumb;
    private String profilUrl;
    private Element html;

    public Post(int id, String content) {
        super(id, content);
    }

    public Post(int id, String content, Element html, String author, String date, String profilThumb, String profilUrl) {
        super(id, content);
        this.author = author;
        this.date = date;
        this.profilThumb = profilThumb;
        this.profilUrl = profilUrl;
        this.html = html;
    }

    public Post(Parcel parcel) {
        id = parcel.readInt();
        content = parcel.readString();
        author = parcel.readString();
        date = parcel.readString();
        profilThumb = parcel.readString();
        profilUrl = parcel.readString();
    }

    public String getAuthor() {
        return author;
    }

    public String getDate() {
        return date;
    }

    public String getProfilThumb() {
        return profilThumb;
    }

    public String getProfilUrl() {
        return profilUrl;
    }

    public Element getHtml() {
        return html;
    }

    public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
        @Override
        public Post createFromParcel(Parcel source) {
            return new Post(source);
        }

        @Override
        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeString(content);
        parcel.writeString(author);
        parcel.writeString(date);
        parcel.writeString(profilThumb);
        parcel.writeString(profilUrl);
    }

    @Override
    public String toString() {
        return content;
    }
}
