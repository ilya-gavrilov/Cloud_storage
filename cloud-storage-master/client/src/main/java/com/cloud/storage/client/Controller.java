package com.cloud.storage.client;

import com.cloud.storage.common.CommandMessage;
import com.cloud.storage.common.FileMessage;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Controller implements Initializable {

    public TextField fieldLogin;
    public PasswordField fieldPassword;
    public Button receiveBtn;
    public Button sendBtn;
    public TableView remoteTable;
    public TableView localTable;
    public ListView logAreaList;
    public Button btnLogout;
    public HBox chooseLocalDirArea;
    public VBox mainArea;
    public VBox loginArea;
    public VBox remoteTableArea;
    public VBox transferBtnArea;
    public VBox localTableArea;
    public VBox logArea;
    public Text localSpaceValue;
    public Text remoteSpaceValue;

    private boolean isAuthorized;
    private boolean isConnected;
    private boolean isLocalDirChoosed;
    public boolean isTransferring;
    private File currentRootDirectory;
    private ConcurrentLinkedQueue<ClientFileSender> sendQueue;
    private ConcurrentHashMap<String, ClientFileReceiver> receiveQueue;
    private ConcurrentHashMap<String, Object[]> pBarList;
    ObservableList<LocalFileListItem> localFileList;
    ObservableList<RemoteFileListItem> remoteFileList;
    Thread connectionListenerThread;
    Thread filesSenderThread;

    public void initialize(URL url, ResourceBundle rb) {
        this.localTableArea.setManaged(false);
        this.localTableArea.setVisible(false);
        this.localTableArea.setPrefWidth(mainArea.getWidth() * 0.45);
        this.remoteTableArea.setManaged(false);
        this.remoteTableArea.setVisible(false);
        this.remoteTableArea.setPrefWidth(mainArea.getWidth() * 0.45);
        this.transferBtnArea.setVisible(false);
        this.transferBtnArea.setManaged(false);
        this.isAuthorized = false;
        this.isConnected = false;
        this.isLocalDirChoosed = false;
        this.isTransferring = false;
        this.sendQueue = new ConcurrentLinkedQueue<>();
        this.receiveQueue = new ConcurrentHashMap<>();
        this.pBarList = new ConcurrentHashMap<>();

        initLocalFilesTable();
        initRemoteFilesTable();

        filesSenderThread = new Thread(() -> {
            try {
                while(!Thread.currentThread().isInterrupted()) {
                    if (!sendQueue.isEmpty()) {
                        sendQueue.poll().sendFile(this);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    writeToLogArea(e.getMessage());
                });
            }
        });
    }

    public void btnConnectClick() {
        try {
            if (fieldLogin.getText().length() != 0 && fieldPassword.getText().length() != 0) {
                if (!isConnected) initConnection();
                ConnectionHandler.getInstance().sendData(new CommandMessage(CommandMessage.AUTH_REQUEST, fieldLogin.getText(), fieldPassword.getText()));

            } else  writeToLogArea("Login or password cannot be empty!!!");

        } catch (IOException e) {
            e.printStackTrace();
            writeToLogArea(e.getMessage());
        }

    }

    public void btnRegisterClick() {
        try {
            if (fieldLogin.getText().length() != 0 && fieldPassword.getText().length() != 0) {
                if (!isConnected) initConnection();
                ConnectionHandler.getInstance().sendData(new CommandMessage(CommandMessage.REGISTER_NEW_USER, fieldLogin.getText(), fieldPassword.getText()));

            } else  writeToLogArea("Login or password cannot be empty!!!");

        } catch (IOException e) {
            e.printStackTrace();
            writeToLogArea(e.getMessage());
        }
    }

    public void initConnection() {
        try {
            ConnectionHandler.getInstance().connect();
            if (ConnectionHandler.getInstance().isConnected()) {
                startConnectionListener();
                isConnected = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            writeToLogArea(e.getMessage());
        }
    }

    public void closeConnection() {
        connectionListenerThread.interrupt();
        ConnectionHandler.getInstance().close();
        isConnected = false;
        isAuthorized = false;
    }

    public void startConnectionListener() {
        connectionListenerThread = new Thread(() -> {
            try {
                while(!Thread.currentThread().isInterrupted()) {
                    Object msg = ConnectionHandler.getInstance().readData();

                    if (msg instanceof CommandMessage) {
                        String command = ((CommandMessage) msg).getCommand();

                        if (command.equals(CommandMessage.AUTH_CONFIRM)) {
                            isAuthorized = true;
                            Platform.runLater(() -> {
                                writeToLogArea("Connected to server OK");
                                initRemoteArea();
                                requestRemoteFileList();
                            });
                            break;
                        }

                        if (command.equals(CommandMessage.AUTH_DECLINE)) {
                            Platform.runLater(() ->
                                    writeToLogArea("Authorization declined by server. Wrong login or password?"));
                        }

                        if (command.equals(CommandMessage.REGISTER_CONFIRM)) {
                            Platform.runLater(() ->
                                writeToLogArea(((CommandMessage) msg).getText())
                            );
                        }

                        if (command.equals(CommandMessage.REGISTER_DECLINE)) {
                            Platform.runLater(() ->
                                writeToLogArea("Registration declined. Try another user name..")
                            );
                        }
                    }
                }

                while (!Thread.currentThread().isInterrupted()) {
                    Object msg = ConnectionHandler.getInstance().readData();

                    if (msg instanceof CommandMessage) {
                        String command = ((CommandMessage) msg).getCommand();

                        if (command.equals(CommandMessage.GET_FILE_LIST)) {
                            Platform.runLater(() -> {
                                updateRemoteTable(((CommandMessage) msg).getFileList(), ((CommandMessage) msg).getAvailableSpace());
                            });
                        }

                        if (command.equals(CommandMessage.DISCONNECT)){
                            Platform.runLater(() -> writeToLogArea("Received disconnect command from server"));
                            break;
                        }

                        if (command.equals(CommandMessage.SEND_FILE_DECLINE_EXIST)) {
                            Platform.runLater(() ->
                                writeToLogArea("File already exist on server: " + ((CommandMessage) msg).getText())
                            );
                        }

                        if (command.equals(CommandMessage.SEND_FILE_DECLINE_SPACE)) {
                            Platform.runLater(() ->
                                writeToLogArea("Not enough space to save file")
                            );
                        }

                        if (command.equals(CommandMessage.SEND_FILE_CONFIRM)) {
                            ClientFileSender fs = new ClientFileSender(new File(currentRootDirectory + "\\" + ((CommandMessage) msg).getFileName()));
                            sendQueue.add(fs);
                            isTransferring = true;
                            fs.sendFile(this);
                        }

                        if (command.equals(CommandMessage.RECEIVE_FILE_DECLINE)) {
                            Platform.runLater(() -> {
                                writeToLogArea("Server declined your file request: " + ((CommandMessage) msg).getText());
                            });
                        }

                        if (command.equals(CommandMessage.RECEIVE_FILE_CONFIRM)) {
                            receiveQueue.put(((CommandMessage) msg).getFileName(), new ClientFileReceiver(currentRootDirectory.getPath(), ((CommandMessage) msg).getFileName(), ((CommandMessage) msg).getFileSize()));
                            ConnectionHandler.getInstance().sendData(new CommandMessage(CommandMessage.RECEIVE_FILE_CONFIRM, ((CommandMessage) msg).getFileName()));
                            isTransferring = true;
                        }

                        if (command.equals(CommandMessage.MESSAGE)) {
                            Platform.runLater(() -> {
                                writeToLogArea("Message from server: " + ((CommandMessage) msg).getText());
                            });
                        }

                    } else if(msg instanceof FileMessage) {
                        ClientFileReceiver fr = receiveQueue.get(((FileMessage) msg).getName());
                        if (fr != null) {
                            boolean isFinished = fr.receiveFile((FileMessage) msg);
                            if (isFinished) {
                                receiveQueue.remove(((FileMessage) msg).getName());
                                Platform.runLater(() -> {
                                    isTransferring = false;
                                    updateLocalTable(currentRootDirectory);
                                    writeToLogArea("File received: " + ((FileMessage) msg).getName());
                                });
                            }
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> writeToLogArea(e.getMessage()));

            } finally {
                ConnectionHandler.getInstance().close();
                isAuthorized = false;
                isConnected = false;
                Platform.runLater(() -> {
                    writeToLogArea("connection closed");
                    hideRemoteArea();
                });
            }
        });
        connectionListenerThread.start();
    }

    public void initRemoteArea() {
        loginArea.setManaged(false);
        loginArea.setVisible(false);
        remoteTableArea.setManaged(true);
        remoteTableArea.setVisible(true);
        fieldLogin.clear();
        fieldPassword.clear();
        if (isLocalDirChoosed) {
            transferBtnArea.setVisible(true);
            transferBtnArea.setManaged(true);
        }
    }

    public void hideRemoteArea() {
        remoteTableArea.setManaged(false);
        remoteTableArea.setVisible(false);
        transferBtnArea.setVisible(false);
        transferBtnArea.setManaged(false);
        loginArea.setManaged(true);
        loginArea.setVisible(true);
    }

    public void addProgressBar(String filename, long fileLength) {
        Text pBarDescr = new Text("Copying: " + filename);
        ProgressBar pBar = new ProgressBar(0);

        HBox hBox = new HBox(10);
        logArea.getChildren().add(hBox);
        hBox.setHgrow(logArea, Priority.ALWAYS);

        pBar.prefWidthProperty().bind(hBox.widthProperty());
        hBox.getChildren().addAll(pBarDescr, pBar);

        pBarList.put(filename, new Object[]{hBox, fileLength});
    }

    public void removeProgressBar(String name) {
        HBox hBox = (HBox) pBarList.get(name)[0];
        pBarList.remove(name);
        logArea.getChildren().remove(hBox);
    }

    public void updateProgressBar(String name, long currentLength) {
        Object[] pBarData = pBarList.get(name);
        ProgressBar pBar = (ProgressBar) ((HBox)pBarData[0]).getChildren().get(1);
        pBar.setProgress((double) (currentLength / (long) pBarData[1]));
    }

    public void writeToLogArea(String text) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy--HH:mm");
        logAreaList.getItems().add(LocalDateTime.now().format(formatter) + " -> " + text);
        logAreaList.scrollTo(logAreaList.getItems().size() - 1);
    }

    public void initLocalFilesTable() {
        TableColumn<LocalFileListItem, String> tcName = new TableColumn<>("Name");
        tcName.setCellValueFactory(new PropertyValueFactory<>("name"));
        tcName.maxWidthProperty().bind(localTable.widthProperty().multiply(0.65));
        tcName.prefWidthProperty().bind(localTable.widthProperty().multiply(0.65));
        tcName.setResizable(false);

        TableColumn<LocalFileListItem, Long> tcSize = new TableColumn<>("Size (KB)");
        tcSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        tcSize.maxWidthProperty().bind(localTable.widthProperty().multiply(0.35));
        tcSize.prefWidthProperty().bind(localTable.widthProperty().multiply(0.35));
        tcSize.setResizable(false);

        localTable.getColumns().addAll(tcName, tcSize);
    }

    public void initRemoteFilesTable() {
        TableColumn<RemoteFileListItem, String> tcName = new TableColumn<>("Name");
        tcName.setCellValueFactory(new PropertyValueFactory<>("name"));
        tcName.maxWidthProperty().bind(remoteTable.widthProperty().multiply(0.65));
        tcName.prefWidthProperty().bind(remoteTable.widthProperty().multiply(0.65));
        tcName.setResizable(false);

        TableColumn<RemoteFileListItem, Integer> tcSize = new TableColumn<>("Size (KB)");
        tcSize.setCellValueFactory(new PropertyValueFactory<>("sizeKB"));
        tcSize.maxWidthProperty().bind(remoteTable.widthProperty().multiply(0.35));
        tcSize.prefWidthProperty().bind(remoteTable.widthProperty().multiply(0.35));
        tcSize.setResizable(false);

        remoteTable.getColumns().addAll(tcName, tcSize);
    }

    public void updateLocalTable(File folder) {
        localTable.getItems().removeAll();
        File[] files = folder.listFiles(pathname -> pathname.isFile());
        localFileList = FXCollections.observableArrayList();

        for(int i = 0; i < files.length; i++) {
            localFileList.add(new LocalFileListItem(files[i].getName(), files[i].length() / 1024, files[i].getAbsolutePath()));
        }

        localTable.setItems(localFileList);
        localSpaceValue.setText(String.valueOf(folder.getFreeSpace() / 1024) + "(KB)");
    }

    public void updateRemoteTable(ArrayList<String[]> files, long availableSpace) {
        remoteFileList = FXCollections.observableArrayList();
        for(int i = 0; i < files.size(); i++) {
            remoteFileList.add(new RemoteFileListItem(files.get(i)[0], Long.parseLong(files.get(i)[1])));
        }

        remoteTable.setItems(remoteFileList);
        remoteSpaceValue.setText(String.valueOf(availableSpace / 1024) + "(KB)");
    }

    public void requestRemoteFileList() {
        try {
            ConnectionHandler.getInstance().sendData(new CommandMessage(CommandMessage.GET_FILE_LIST));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestFileSending(File file) {
        writeToLogArea("Request sending file:  " + file.getAbsolutePath());
        try {
            ConnectionHandler.getInstance().sendData(new CommandMessage(CommandMessage.SEND_FILE_REQUEST, file.getName(), file.length()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestFile(String name, long fileSize) {
        for(File file : currentRootDirectory.listFiles()) {
            if (file.getName().equals(name)) {
                writeToLogArea("File already exist in local storage: " + name);
                return;
            }
        }

        if (fileSize < (currentRootDirectory.getFreeSpace() + 2000000)) {
            try {
                writeToLogArea("Requesting file from server: " + name);
                ConnectionHandler.getInstance().sendData(new CommandMessage(CommandMessage.RECEIVE_FILE_REQUEST, name, fileSize));
            } catch (IOException e) {
                e.printStackTrace();
                writeToLogArea(e.getMessage());
            }
        } else {
            writeToLogArea("Not enough space in local storage");
        }
    }

    public void requestRemoteFileDelete(String fileName) {
        try {
            ConnectionHandler.getInstance().sendData(new CommandMessage(CommandMessage.DELETE_FILE_REQUEST, fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void requestRemoteFileRename(String oldFileName, String newFileName) {
        try {
            ConnectionHandler.getInstance().sendData(new CommandMessage(CommandMessage.RENAME_FILE_REQUEST, oldFileName, newFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void btnSendClick() {
        if(isTransferring) {
            writeToLogArea("Transfer in progress. Please wait...");
            return;
        }
        LocalFileListItem fileItem = (LocalFileListItem) localTable.getSelectionModel().getSelectedItem();
        requestFileSending(new File(fileItem.getPath()));
    }

    public void btnReceiveClick() {
        if(isTransferring) {
            writeToLogArea("Transfer in progress. Please wait...");
            return;
        }
        RemoteFileListItem fileItem = (RemoteFileListItem) remoteTable.getSelectionModel().getSelectedItem();
        requestFile(fileItem.getName(), fileItem.getSizeByte());
    }

    public void btnLogoutClick() {
        if(isTransferring) {
            writeToLogArea("Transfer in progress. Please wait...");
            return;
        }
        ConnectionHandler.getInstance().close();
        isAuthorized = false;
        writeToLogArea("initiating logout");
        writeToLogArea("connection closed by user");
        hideRemoteArea();
    }

    public void btnRefreshRemote() {
        if(isTransferring) {
            writeToLogArea("Transfer in progress. Please wait...");
            return;
        }
        requestRemoteFileList();
    }

    public void btnRefreshLocal() {
        updateLocalTable(currentRootDirectory);
    }

    public void btnChangeDirClick() {
        if(isTransferring) {
            writeToLogArea("Transfer in progress. Please wait...");
            return;
        }
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select local directory");
        File selectedDirectory = chooser.showDialog(Main.primaryStage);
        if (selectedDirectory != null) {
            currentRootDirectory = selectedDirectory;
            updateLocalTable(selectedDirectory);
        }
    }

    public void btnChooseDirClick() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select local directory");
        File selectedDirectory = chooser.showDialog(Main.primaryStage);

        if (selectedDirectory != null) {
            currentRootDirectory = selectedDirectory;
            updateLocalTable(selectedDirectory);
            isLocalDirChoosed = true;
            chooseLocalDirArea.setManaged(false);
            chooseLocalDirArea.setVisible(false);
            localTableArea.setManaged(true);
            localTableArea.setVisible(true);
            if (isAuthorized) {
                transferBtnArea.setVisible(true);
                transferBtnArea.setManaged(true);
            }
        }
    }

    public void btnDeleteLocalFile() {
        LocalFileListItem fileItem = (LocalFileListItem) localTable.getSelectionModel().getSelectedItem();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you really want to delete file " + fileItem.getName(), ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get().getText().equals("OK")) {
            (new File(fileItem.getPath())).delete();
            updateLocalTable(currentRootDirectory);
            writeToLogArea("File " + fileItem.getName() + " was deleted");
        }
    }

    public void btnRenameLocalFile() {
        LocalFileListItem fileItem = (LocalFileListItem) localTable.getSelectionModel().getSelectedItem();
        TextInputDialog textInputDialog = new TextInputDialog(fileItem.getName());
        textInputDialog.setTitle("Rename file");
        textInputDialog.setHeaderText("Enter new name:");
        Optional<String> result = textInputDialog.showAndWait();
        if (result.isPresent() && !result.get().equals(fileItem.getName())) {
            (new File(fileItem.getPath())).renameTo(new File(currentRootDirectory + "\\" + result.get()));
            updateLocalTable(currentRootDirectory);
        }
    }

    public void btnRenameRemoteFile() {
        if(isTransferring) {
            writeToLogArea("Transfer in progress. Please wait...");
            return;
        }
        RemoteFileListItem fileItem = (RemoteFileListItem) remoteTable.getSelectionModel().getSelectedItem();
        TextInputDialog textInputDialog = new TextInputDialog(fileItem.getName());
        textInputDialog.setTitle("Rename file");
        textInputDialog.setHeaderText("Enter new name:");
        Optional<String> result = textInputDialog.showAndWait();
        if (result.isPresent() && !result.get().equals(fileItem.getName())) {
            requestRemoteFileRename(fileItem.getName(), result.get());
        }
    }

    public void btnDeleteRemoteFile() {
        if(isTransferring) {
            writeToLogArea("Transfer in progress. Please wait...");
            return;
        }
        RemoteFileListItem fileItem = (RemoteFileListItem) remoteTable.getSelectionModel().getSelectedItem();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Do you really want to delete file " + fileItem.getName(), ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get().getText().equals("OK")) {
            requestRemoteFileDelete(fileItem.getName());
        }
    }
}
