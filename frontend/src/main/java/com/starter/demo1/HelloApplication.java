package com.starter.demo1;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;

public class HelloApplication extends Application {

    public static String name;

    @Override
    public void start(Stage stage) throws IOException {
        showDialog();
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("ChatView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
    }

    private void showDialog() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Ввод имени");
        dialog.setHeaderText(null);
        dialog.setContentText("Введите ваше имя:");

        dialog.showAndWait().ifPresent(user -> {
            System.out.println("Пользователь ввел: " + user);
            this.name = user;
        });
    }

    public static void main(String[] args) {
        launch();
    }
}