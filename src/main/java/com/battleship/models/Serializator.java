package com.battleship.models;

import java.io.*;

public class Serializator {

    private static final String SERIAL_FILE = "battleship_data.ser";
    private static final String TEXT_FILE = "battleship_status.txt";

    // AHORA RECIBE 'int timeSeconds'
    public static boolean saveGame(Board playerBoard, Board enemyBoard, String nickname, int timeSeconds) {
        try {
            // 1. SERIALIZACIÓN: Guardamos los objetos complejos
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SERIAL_FILE))) {
                oos.writeObject(playerBoard);
                oos.writeObject(enemyBoard);
                oos.writeInt(timeSeconds); // <--- GUARDAMOS EL TIEMPO
            }

            // 2. ARCHIVO PLANO
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(TEXT_FILE))) {
                writer.write("Nickname: " + nickname);
                writer.newLine();
                writer.write("Player_Sunk_Ships: " + playerBoard.getShotsFired().size());
                writer.newLine();
                writer.write("Time_Elapsed_Seconds: " + timeSeconds); // <--- GUARDAMOS EL TIEMPO EN TEXTO TAMBIÉN
            }

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static GameDTO loadGame() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SERIAL_FILE))) {
            Board playerBoard = (Board) ois.readObject();
            Board enemyBoard = (Board) ois.readObject();
            int savedTime = ois.readInt(); // <--- LEEMOS EL TIEMPO

            String nickname = "Unknown";
            try (BufferedReader reader = new BufferedReader(new FileReader(TEXT_FILE))) {
                String line = reader.readLine();
                if (line != null && line.startsWith("Nickname: ")) {
                    nickname = line.substring(10);
                }
            }

            return new GameDTO(playerBoard, enemyBoard, nickname, savedTime);

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}