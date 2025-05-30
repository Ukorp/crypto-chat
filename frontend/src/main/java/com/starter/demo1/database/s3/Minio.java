package com.starter.demo1.database.s3;

import com.starter.demo1.service.MessageService;
import com.starter.demo1.crypto.CipherContext;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.SneakyThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public abstract class Minio {

    private Minio() {
    }

    private static MinioClient client = MinioClient.builder()
            .endpoint("http://localhost:9000/")
            .credentials("minioadmin", "minioadmin")
            .build();


    @SneakyThrows
    public static CompletableFuture<Void> uploadFile(String sessionId, String path, Consumer<String> onFileLoad) {
        if (sessionId == null) {
            System.out.println("sessionId is null");
            return CompletableFuture.completedFuture(null);
        }

        Path inputFile = Path.of(path);

        String fileName = inputFile.getFileName().toString();

        int dotIndex = fileName.lastIndexOf('.');
        String nameWithoutExt = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
        String extension = (dotIndex == -1) ? "" : fileName.substring(dotIndex);

        String newFileName = nameWithoutExt + "_encrypted" + extension;

        Path outputFile = inputFile.resolveSibling(newFileName);

        if (!Files.exists(outputFile)) {
            Files.createFile(outputFile);
            System.out.println("Файл создан: " + outputFile);
        } else {
            System.out.println("Файл уже существует: " + outputFile);
        }

        CipherContext cipher = MessageService.sessionCipher.get(sessionId);
        if (cipher == null) {
            System.out.println("cipher is null");
            return CompletableFuture.completedFuture(null);
        }
        return cipher.encryptFileAsync(inputFile, outputFile).thenAccept(e ->
                {
                    try {
                        client.uploadObject(
                                UploadObjectArgs.builder()
                                        .bucket("files")
                                        .object(sessionId + "_" + fileName)
                                        .filename(outputFile.toString())
                                        .build());
                    } catch (Exception err) {
                        err.printStackTrace();
                    }
                }
        ).thenAccept(avoid -> {
            onFileLoad.accept(fileName);
            try {
                Files.delete(outputFile);
            } catch (IOException e) {
                System.out.println("Чето странное");
            }
        });
    }

    @SneakyThrows
    public static CompletableFuture<Void> getFile(String sessionId, String filename) {
        if (sessionId == null) {
            System.out.println("sessionId is null");
            return CompletableFuture.completedFuture(null);
        }
        Path path = Path.of(filename);
        if (Files.exists(path)) {
            return CompletableFuture.completedFuture(null);
        }

        String encryptedFile = sessionId + "_" + filename;

        Path tmpPath = Path.of("tmp_encrypted_" + filename);
        Files.createFile(tmpPath);
        if (!Files.exists(path)) {
            Files.createFile(path);
            System.out.println("Файл создан: " + filename);
        } else {
            System.out.println("Файл уже существует: " + filename);
        }

        CipherContext cipher = MessageService.sessionCipher.get(sessionId);
        if (cipher == null) {
            System.out.println("cipher is null");
            return CompletableFuture.completedFuture(null);
        }
        GetObjectResponse response = client.getObject(GetObjectArgs.builder()
                .bucket("files")
                .object(encryptedFile)
                .build());
        Files.copy(response, tmpPath, StandardCopyOption.REPLACE_EXISTING);
        return cipher.decryptFileAsync(tmpPath, path)
                .thenAccept(avoid -> {
                    try {
                        Files.delete(tmpPath);
                    } catch (IOException e) {
                        System.out.println("чет не то");
                    }
                });
    }
}
