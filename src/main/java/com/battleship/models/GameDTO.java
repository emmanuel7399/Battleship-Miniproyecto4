package com.battleship.models;

import java.io.Serializable;

public class GameDTO implements Serializable {
    private Board playerBoard;
    private Board enemyBoard;
    private String nickname;
    private int elapsedSeconds;

    public GameDTO(Board playerBoard, Board enemyBoard, String nickname, int elapsedSeconds) {
        this.playerBoard = playerBoard;
        this.enemyBoard = enemyBoard;
        this.nickname = nickname;
        this.elapsedSeconds = elapsedSeconds;
    }

    public Board getPlayerBoard() { return playerBoard; }
    public Board getEnemyBoard() { return enemyBoard; }
    public String getNickname() { return nickname; }
    public int getElapsedSeconds() { return elapsedSeconds; }
}