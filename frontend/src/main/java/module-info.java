module com.starter.demo1 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires org.java_websocket;
    requires org.json;

    // gRPC и Protocol Buffers зависимости
    requires io.grpc;
    requires io.grpc.stub;
    requires io.grpc.protobuf;
    requires com.google.protobuf;
    requires java.annotation;
    requires static lombok;
    requires com.google.common;
    requires annotations;
    requires org.jooq;
    requires com.zaxxer.hikari;
    requires jdk.compiler;
    requires minio;
    requires java.desktop;

    opens com.starter.demo1 to javafx.fxml;
    exports com.starter.demo1;
    exports com.starter.protomeme.chat;
    exports com.starter.demo1.controller;
    opens com.starter.demo1.controller to javafx.fxml;
    exports com.starter.demo1.database.sqlite;
    opens com.starter.demo1.database.sqlite to javafx.fxml;
    exports com.starter.demo1.database.s3;
    opens com.starter.demo1.database.s3 to javafx.fxml;
    exports com.starter.demo1.util;
    opens com.starter.demo1.util to javafx.fxml;
    exports com.starter.demo1.service;
    opens com.starter.demo1.service to javafx.fxml;
    exports com.starter.demo1.model;
    opens com.starter.demo1.model to javafx.fxml;
}