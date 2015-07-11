package com.pentapenguin.jvcbrowser.entities;

import android.os.Parcel;
import android.os.Parcelable;

public class Mp extends Topic {

    public Mp(int id, String content) {
        super(id, content);
    }

    public Mp(int id, int code, int idForum, String content, String author, String lastPostDate, String thumbUrl,
              int postsNumber) {
        super(id, code, idForum, content, author, lastPostDate, thumbUrl, postsNumber);
    }

    public Mp(Parcel parcel) {
        super(parcel);
    }

    public static final Parcelable.Creator<Mp> CREATOR = new Parcelable.Creator<Mp>() {
        @Override
        public Mp createFromParcel(Parcel source) {
            return new Mp(source);
        }

        @Override
        public Mp[] newArray(int size) {
            return new Mp[size];
        }
    };
}
