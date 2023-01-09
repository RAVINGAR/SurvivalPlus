package com.ravingarinc.survival.file.sql;

public class Schema {
    public static final String PLAYERS = "players";

    public static final String createTable = "CREATE TABLE IF NOT EXISTS " + PLAYERS + " (" +
            Player.UUID + " TEXT PRIMARY KEY," +
            Player.TEMPERATURE + " REAL NOT NULL)";

    public static class Player {

        public static final String UUID = "uuid";
        public static final String TEMPERATURE = "temperature";
        public static final String select = "SELECT *" +
                " FROM " + PLAYERS +
                " WHERE " + Player.UUID + " = ?";
        public static final String insert = "INSERT INTO " + PLAYERS + "(" +
                Player.UUID + "," + Player.TEMPERATURE + ")" +
                "VALUES(?,?)";
        public static final String update = "UPDATE " + PLAYERS +
                " SET " + Player.TEMPERATURE + " = ?" +
                " WHERE " + Player.UUID + " = ?";
    }
}
