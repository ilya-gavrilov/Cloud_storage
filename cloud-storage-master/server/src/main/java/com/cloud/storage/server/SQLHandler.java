package com.cloud.storage.server;

import java.sql.*;

public class SQLHandler {
    private static Connection connection;
    private static Statement stmt;

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:usersDB.db");
            stmt = connection.createStatement();
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public static String getUserFolderByLoginAndPassword(String login, String password) {
        try {
            ResultSet rs = stmt.executeQuery("SELECT userDirPath FROM users WHERE login = '" + login + "' AND password = '" + password + "';");
            if(rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getUserFolderByLogin(String login) {
        try {
            ResultSet rs = stmt.executeQuery("SELECT userDirPath FROM users WHERE login = '" + login + "';");
            if(rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean updateUserDirPath(String login, String userDirPath) {
        try {
            stmt.executeUpdate(String.format("UPDATE USERS SET userDirPath = '%s' WHERE login = '%s'", userDirPath, login));
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean tryToRegister(String login, String password, String userDirPath) {
        try {
            stmt.executeUpdate(String.format("INSERT INTO users (login, password, userDirPath) VALUES ('%s', '%s', '%s');", login, password, userDirPath));
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
