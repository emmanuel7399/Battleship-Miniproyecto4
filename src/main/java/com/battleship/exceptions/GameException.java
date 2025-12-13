package com.battleship.exceptions;

public class GameException extends RuntimeException {

    private final ErrorType type;

    public GameException(ErrorType type, String message) {
        super(message);
        this.type = type;
    }

    public ErrorType getType() {
        return type;
    }
}
