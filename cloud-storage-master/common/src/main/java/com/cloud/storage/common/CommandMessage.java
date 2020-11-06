package com.cloud.storage.common;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class CommandMessage implements Serializable {

    public static final String AUTH_REQUEST = "14456325432875";
    public static final String AUTH_CONFIRM = "13456546754769";
    public static final String AUTH_DECLINE = "13421543364567";
    public static final String REGISTER_NEW_USER = "253256898245";
    public static final String REGISTER_CONFIRM = "253256898245";
    public static final String REGISTER_DECLINE = "24676686578245";
    public static final String DISCONNECT = "42391363456346";
    public static final String GET_FILE_LIST = "584772838686292475";
    public static final String SEND_FILE_REQUEST = "3649926666549";
    public static final String SEND_FILE_CONFIRM = "359637355478568";
    public static final String SEND_FILE_DECLINE_EXIST = "335875769863965";
    public static final String SEND_FILE_DECLINE_SPACE = "345654632314453";
    public static final String RECEIVE_FILE_REQUEST = "869465376974849";
    public static final String RECEIVE_FILE_CONFIRM = "286759878629387";
    public static final String RECEIVE_FILE_DECLINE = "9876857632948734";
    public static final String DELETE_FILE_REQUEST = "676653248734576532";
    public static final String RENAME_FILE_REQUEST = "701883784759967756";
    public static final String MESSAGE = "9949495653775";


    private String command;
    private String text;
    private String login;
    private String password;
    private ArrayList<String[]> fileList;
    private String fileName;
    private Long fileSize;
    private long availableSpace;


    public CommandMessage(String command, ArrayList<String[]> fileList, long availableSpace) {
        this.command = command;
        this.fileList = fileList;
        this.availableSpace = availableSpace;
    }

    public CommandMessage(String command, String fileName, long fileSize) {
        this.command = command;
        this.fileName = fileName;
        this.fileSize = fileSize;
    }

    public CommandMessage(String command, String text) {
        this.command = command;
        this.text = text;
    }

    public CommandMessage(String command) {
        this.command = command;
    }

    public CommandMessage(String command, String login, String password) {
        this.command = command;
        this.login = login;
        this.password = password;
    }

    public String getCommand() {
        return command;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getText() {
        return text;
    }

    public ArrayList<String[]> getFileList() {
        return fileList;
    }

    public String getFileName() {
        return fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public long getAvailableSpace() {
        return availableSpace;
    }
}
