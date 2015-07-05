package com.pentapenguin.jvcbrowser.exceptions;

public class NoContentFoundException extends Exception {

    public NoContentFoundException() {
        super("Aucun contenu !");
    }

    public NoContentFoundException(String detailMessage) {
        super(detailMessage);
    }
}
