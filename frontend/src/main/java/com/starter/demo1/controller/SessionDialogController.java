package com.starter.demo1.controller;

import com.google.protobuf.ByteString;
import com.starter.demo1.HelloApplication;
import com.starter.demo1.service.MessageService;
import com.starter.demo1.model.SessionCredentials;
import com.starter.demo1.crypto.cipher.dh.DiffieHellman;
import com.starter.protomeme.chat.CipherMode;
import com.starter.protomeme.chat.Padding;
import com.starter.protomeme.chat.SessionRequest;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.Setter;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class SessionDialogController {

    private final DiffieHellman diffieHellman = new DiffieHellman();

    @FXML private ComboBox<CipherMode> cipherModeBox;
    @FXML private ComboBox<Padding> paddingBox;
    @FXML private ComboBox<String> ciphers;
    @FXML private TextField usernameField;

    @Setter
    private Stage dialogStage;
    @Setter
    private Consumer<SessionRequest> onSubmit;

    @FXML
    public void initialize() {
        cipherModeBox.getItems().addAll(CipherMode.values());
        paddingBox.getItems().addAll(Padding.values());
        ciphers.getItems().addAll(List.of("MAGENTA", "LOKI97"));
    }

    @FXML
    private void handleSubmit(ActionEvent event) {
        BigInteger p = diffieHellman.generateSafePrime(300);
        BigInteger g = diffieHellman.findPrimitiveRoot(p);
        BigInteger closeA = diffieHellman.generateCloseA(100);
        BigInteger openA = diffieHellman.generatePublicA(closeA, p, g);
        SessionCredentials elem = new SessionCredentials(
                HelloApplication.name,
                usernameField.getText(),
                closeA,
                g,
                p
        );
        String sessionId = UUID.randomUUID().toString();
        MessageService.credentials.put(sessionId, elem);
        SessionRequest request = SessionRequest.newBuilder()
                .setSessionId(sessionId)
                .setCipher(ciphers.getValue())
                .setFrom(elem.getFrom())
                .setTo(elem.getTo())
                .setCipherMode(cipherModeBox.getValue())
                .setPadding(paddingBox.getValue())
                .setPublicG(ByteString.copyFrom(g.toByteArray()))
                .setPublicP(ByteString.copyFrom(p.toByteArray()))
                .setPublicA(ByteString.copyFrom(openA.toByteArray()))
                .build();

        if (onSubmit != null) {
            Platform.runLater(() -> onSubmit.accept(request));
        }
        dialogStage.close();
    }
}
