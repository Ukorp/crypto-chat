package com.starter.protomeme.service;

import com.starter.protomeme.chat.MessageStatus;
import com.starter.protomeme.model.MessageEntity;
import com.starter.protomeme.repository.MessageRepository;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    @SneakyThrows
    public void removeFiles(String sessionId) {
        var list = messageRepository.findAllBySessionIdAndStatus(sessionId, MessageStatus.FILE);
        for (MessageEntity message : list) {
            String fileName = sessionId + "_" + new String(message.getMessage());
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .build());
        }
    }

    @Transactional
    public void deleteAllBySessionId(String sessionId) {
        removeFiles(sessionId);
        messageRepository.removeAllBySessionId(sessionId);
    }
}
