package com.starter.protomeme.repository;


import com.starter.protomeme.chat.MessageStatus;
import com.starter.protomeme.chat.Status;
import com.starter.protomeme.model.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {

    Iterable<MessageEntity> findAllBySessionIdOrderByTimestampAsc(String sessionId);

    Iterable<MessageEntity> findAllBySessionIdAndStatus(String sessionId, MessageStatus status);

    void removeAllBySessionId(String sessionId);
}
