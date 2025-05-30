package com.starter.protomeme;


import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.starter.protomeme.chat.*;
import com.starter.protomeme.model.MessageEntity;
import com.starter.protomeme.repository.MessageRepository;
import com.starter.protomeme.service.MessageService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.cglib.core.Local;
import org.springframework.grpc.server.service.GrpcService;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@GrpcService
@RequiredArgsConstructor
@Slf4j
public class ChatService extends ChatServiceGrpc.ChatServiceImplBase {

    private final ConcurrentMap<String, StreamObserver<ChatMessage>> streamMap;

    private final ConcurrentMap<String, SessionInfo> sessionMap;

    private final ConcurrentMap<String, StreamObserver<SessionRequest>> listeners = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, StreamObserver<SessionResponse>> responsers = new ConcurrentHashMap<>();

    private final MessageRepository messageRepository;

    private final MessageService messageService;

    @Override
    public void makeSession(SessionRequest request, StreamObserver<SessionResponse> responseObserver) {
        var elem = listeners.get(request.getTo());
        if (elem == null) {
            System.out.println("Не найден пользователь такой: " + request.getFrom());
            return;
        }
        elem.onNext(request);
        responsers.put(request.getFrom(), responseObserver);
    }

    @Override
    public StreamObserver<ChatMessage> chatStream(StreamObserver<ChatMessage> responseObserver) {
        return new StreamObserver<>() {

            String user = null;

            @Override
            public void onNext(ChatMessage chatMessage) {
                log.info("Заявка на отправку сообщения");
                String currentSession = chatMessage.getSessionId();
                user = chatMessage.getSender();
                streamMap.put(user, responseObserver);
                if (!sessionMap.containsKey(currentSession)) {
                    log.warn("Сессия не найдена");
                    ChatMessage messageEntity = ChatMessage.newBuilder()
                            .setSessionId(currentSession)
                            .setMessageStatus(MessageStatus.SESSION_NOT_FOUND)
                            .build();
                    responseObserver.onNext(messageEntity);
                    return;
                }
                MessageEntity messageEntity = MessageEntity.builder()
                        .sessionId(currentSession)
                        .sender(chatMessage.getSender())
                        .recipient(chatMessage.getRecipient())
                        .timestamp(LocalDateTime.now())
                        .status(chatMessage.getMessageStatus())
                        .message(chatMessage.getMessage().toByteArray())
                        .build();
                messageRepository.save(messageEntity);
                var toUser = streamMap.get(chatMessage.getRecipient());
                if (toUser != null) {
                    log.info("Отправляем");
                    toUser.onNext(chatMessage);
                } else {
                    log.warn("чет нет observer: {}", chatMessage.getRecipient());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                if (user != null) {
                    streamMap.remove(user);
                }
            }

            @Override
            public void onCompleted() {
                if (user != null) {
                    streamMap.remove(user);
                }
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void sessionListener(SessionListenerRequest request, StreamObserver<SessionRequest> responseObserver) {
        listeners.put(request.getUserId(), responseObserver);
    }

    @Override
    public void sessionCreated(SessionResponse request, StreamObserver<SessionResponse> responseObserver) {
        sessionMap.put(request.getSessionId(), request.getSessionInfo());
        responsers.get(request.getSessionInfo().getRecipient()).onNext(request);
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }

    @Override
    public void getSessionMessages(GetSessionsRequest request, StreamObserver<ChatMessage> responseObserver) {
        Iterable<MessageEntity> messageEntities = messageRepository.findAllBySessionIdOrderByTimestampAsc(request.getSessionId());
        for (MessageEntity messageEntity : messageEntities) {

            ChatMessage messageDto = getMessageDto(messageEntity);
            responseObserver.onNext(messageDto);
        }
        responseObserver.onCompleted();
    }

    @NotNull
    private static ChatMessage getMessageDto(MessageEntity messageEntity) {
        LocalDateTime timestamp = messageEntity.getTimestamp();
        Timestamp timestampDto = Timestamp.newBuilder()
                .setSeconds(timestamp.toEpochSecond(ZoneOffset.UTC))
                .setNanos(timestamp.getNano())
                .build();
        return ChatMessage.newBuilder()
                .setSessionId(messageEntity.getSessionId())
                .setSender(messageEntity.getSender())
                .setMessageStatus(messageEntity.getStatus())
                .setRecipient(messageEntity.getRecipient())
                .setMessage(ByteString.copyFrom(messageEntity.getMessage()))
                .setTimestamp(timestampDto)
                .build();
    }

    @Override
    public void closeSession(CloseSessionRequest request, StreamObserver<CloseSessionRequest> responseObserver) {
        sessionMap.remove(request.getSessionId());
        messageService.deleteAllBySessionId(request.getSessionId());
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }
}
