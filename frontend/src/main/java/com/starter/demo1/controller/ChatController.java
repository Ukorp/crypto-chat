
package com.starter.demo1.controller;

import com.google.protobuf.ByteString;
import com.starter.demo1.*;
import com.starter.demo1.crypto.CipherContext;
import com.starter.demo1.database.s3.Minio;
import com.starter.demo1.database.sqlite.DatabaseConnectionManager;
import com.starter.demo1.service.MessageService;
import com.starter.demo1.util.CipherFactory;
import com.starter.protomeme.chat.CipherMode;
import com.starter.protomeme.chat.Padding;
import com.starter.protomeme.chat.SessionInfo;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jooq.DSLContext;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

public class ChatController {

    @FXML
    public HBox onlineUsersHbox;

    @FXML
    public ListView<SessionInfo> userList;

    public static String currentSession;

    @FXML
    private TextField messageField;

    @FXML
    private Button buttonSend;

    @FXML
    private Button closeSessionButton;

    @FXML
    private Button fileButton;

    @FXML
    private ListView<String> messageList;
    @FXML
    private ListView<TextFlow> chatPane;
    @FXML
    private TextArea messageBox;

    private MessageService messageService;

    @FXML
    public void initialize() {
        loadSessionList();
        messageService = new MessageService("localhost", 9090);
        messageService.sessionListener(elem -> Platform.runLater(() -> userList.getItems().add(elem)));
        messageService.chatStream(msg -> Platform.runLater(() -> chatPane.getItems().add(textFlowFromString(msg))),
                err -> Platform.runLater(() -> userList.getItems().removeIf(element ->
                        element.getSessionId().equals(err)
                )),
                file -> Platform.runLater(() -> addFileToChat(new File(file))));
        userList.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        loadChatForUser(newVal);
                    }
                });
        buttonSend.disableProperty().bind(Bindings.isNull(userList.getSelectionModel().selectedItemProperty()));
        fileButton.disableProperty().bind(Bindings.isNull(userList.getSelectionModel().selectedItemProperty()));
        closeSessionButton.disableProperty().bind(Bindings.isNull(userList.getSelectionModel().selectedItemProperty()));
        messageService.sendMessage(" ", " ");
    }

    private void addFileToChat(File file) {
        System.out.println("Добавляю " + file.getName());
        TextFlow textFlow = new TextFlow();
        Hyperlink hyperlink = new Hyperlink(file.getName());
        hyperlink.setOnAction(event -> {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                System.out.println("Файл то не найден");
            }
        });
        textFlow.getChildren().add(hyperlink);
        chatPane.getItems().add(textFlow);
    }

    private TextFlow textFlowFromString(String text) {
        TextFlow textFlow = new TextFlow();
        textFlow.getChildren().add(new Text(text));
        return textFlow;
    }

    private void loadSessionList() {
        DSLContext dsl = DatabaseConnectionManager.getDslContext();
        var sessions = dsl.selectFrom(table("sessions")).fetchArray();
        for (var session : sessions) {
            String sessionId = session.get(field("id"), String.class);
            String sender = session.get(field("sender"), String.class);
            String recipient = session.get(field("recipient"), String.class);
            String cipherName = session.get(field("cipher_name"), String.class);
            byte[] key = session.get(field("key"), byte[].class);
            CipherMode mode = session.get(field("cipher_mode"), CipherMode.class);
            Padding padding = session.get(field("padding"), Padding.class);
            byte[] iv = session.get(field("iv"), byte[].class);

            SessionInfo addInfo = SessionInfo.newBuilder()
                    .setCipher(cipherName)
                    .setPadding(padding)
                    .setCipherMode(mode)
                    .setSessionId(sessionId)
                    .setSender(HelloApplication.name)
                    .setRecipient(recipient)
                    .setSender(sender)
                    .setIv(ByteString.copyFrom(iv))
                    .build();

            CipherContext cipher = CipherFactory.createCipher(addInfo, key);
            MessageService.sessions.put(sessionId, addInfo);
            Platform.runLater(() -> userList.getItems().add(addInfo));
            MessageService.sessionCipher.put(sessionId, cipher);
        }
    }

    private void loadChatForUser(SessionInfo newVal) {
        messageBox.clear();
        chatPane.getItems().clear();
        messageService.getSessionMessages(newVal.getSessionId(),
                elem -> Platform.runLater(() -> chatPane.getItems().add(textFlowFromString(elem))),
                file -> Platform.runLater(() -> addFileToChat(new File(file))));
        System.out.println(newVal.getSessionId());
        currentSession = newVal.getSessionId();
    }

    public void sendMethod(KeyEvent keyEvent) {
    }

    public void sendButtonAction(ActionEvent actionEvent) {
        chatPane.getItems().add(textFlowFromString(HelloApplication.name + ": " + messageBox.getText()));
        messageService.sendMessage(currentSession, messageBox.getText());
        messageBox.clear();
    }

    public void addChat(ActionEvent actionEvent) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SessionDialog.fxml"));
            Parent root = loader.load();

            SessionDialogController controller = loader.getController();
            Stage dialogStage = new Stage();
            controller.setDialogStage(dialogStage);
            controller.setOnSubmit(request ->
                    messageService.makeSession(request,
                            e -> Platform.runLater(() -> userList.getItems().add(e))
                    ));

            dialogStage.setScene(new Scene(root));
            dialogStage.setTitle("Новый чат");
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeSession(ActionEvent actionEvent) {
        if (currentSession != null) {
            messageService.closeSession(currentSession,
                    id -> Platform.runLater(() -> userList.getItems().removeIf(element ->
                            element.getSessionId().equals(id)
                    )));
            messageBox.clear();
            chatPane.getItems().clear();
        }
    }

    public void uploadFile(ActionEvent actionEvent) {

        // Создаем метку для отображения выбранного файла
        Label selectedFileLabel = new Label("Файл не выбран");

        Stage window = (Stage) messageBox.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Выбор файла");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Текстовые файлы", "*.txt"),
                new FileChooser.ExtensionFilter("Изображения", "*.png", "*.jpg", "*.gif"),
                new FileChooser.ExtensionFilter("Все файлы", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(window);

        if (selectedFile != null) {
            selectedFileLabel.setText("Выбран файл: " + selectedFile.getAbsolutePath());
            Minio.uploadFile(currentSession, selectedFile.getAbsolutePath(), filename ->
                Platform.runLater(() -> addFileToChat(selectedFile)
            )).thenAccept(avoid -> messageService.sendFile(currentSession, selectedFile.getName()));
        } else {
            selectedFileLabel.setText("Файл не выбран");
        }
    }
}
