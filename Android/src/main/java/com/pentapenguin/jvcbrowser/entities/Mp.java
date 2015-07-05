package com.pentapenguin.jvcbrowser.entities;

import android.os.Parcel;

public class Mp extends Topic{

    public Mp(int id, String content) {
        super(id, content);
    }

    public Mp(int id, int code, int idForum, int page) {
        super(id, code, idForum, page);
    }

    public Mp(int id, int code, int idForum) {
        super(id, code, idForum);
    }

    public Mp(int id, int code, int idForum, String content, String author, String lastPostDate, String thumbUrl, int postsNumber) {
        super(id, code, idForum, content, author, lastPostDate, thumbUrl, postsNumber);
    }

    public Mp(int id, int code, int idForum, String content, int page, String author, String lastPostDate, String thumbUrl, int postsNumber) {
        super(id, code, idForum, content, page, author, lastPostDate, thumbUrl, postsNumber);
    }

    public Mp(Parcel parcel) {
        super(parcel);
    }
}
