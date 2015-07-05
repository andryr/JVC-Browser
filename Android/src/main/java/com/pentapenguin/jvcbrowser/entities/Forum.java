package com.pentapenguin.jvcbrowser.entities;

import android.os.Parcel;
import android.os.Parcelable;

public class Forum extends Item implements Parcelable {

    private int page = 1;

    public Forum(int id, int page, String content) {
        super(id, content);
        this.page = page;
    }

    public Forum(int id, String content) {
        super(id, content);
    }

    public Forum(Parcel parcel) {
        id = parcel.readInt();
        page = parcel.readInt();
        content = parcel.readString();
    }

    public int getPage() {
        return page;
    }

    public static final Parcelable.Creator<Forum> CREATOR = new Parcelable.Creator<Forum>() {
        @Override
        public Forum createFromParcel(Parcel source) {
            return new Forum(source);
        }

        @Override
        public Forum[] newArray(int size) {
            return new Forum[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(id);
        parcel.writeInt(page);
        parcel.writeString(content);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Forum && ((Forum) o).getId() == id;

    }

    @Override
    public String toString() {
        return super.toString() + ", page: ";
    }
}
