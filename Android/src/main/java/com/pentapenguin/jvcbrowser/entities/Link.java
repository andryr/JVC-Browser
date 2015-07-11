package com.pentapenguin.jvcbrowser.entities;

import android.os.Parcel;
import android.os.Parcelable;

public class Link implements Parcelable {

    private String url;
    private String content;

    public Link(String url, String content) {
        this.url = url;
        this.content = content;
    }

    public Link(Parcel p) {
        url = p.readString();
        content = p.readString();
    }

    public String getUrl() {
        return url;
    }

    public String getContent() {
        return content;
    }

    public static final Parcelable.Creator<Link> CREATOR = new Parcelable.Creator<Link>() {
        @Override
        public Link createFromParcel(Parcel source) {
            return new Link(source);
        }

        @Override
        public Link[] newArray(int size) {
            return new Link[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(url);
        dest.writeString(content);
    }
}
