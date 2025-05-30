package com.starter.demo1.service;

import com.google.protobuf.ByteString;
import com.starter.demo1.HelloApplication;
import com.starter.demo1.model.SessionCredentials;
import com.starter.demo1.controller.ChatController;
import com.starter.demo1.crypto.CipherContext;
import com.starter.demo1.crypto.cipher.dh.DiffieHellman;
import com.starter.demo1.database.s3.Minio;
import com.starter.demo1.database.sqlite.DatabaseConnectionManager;
import com.starter.demo1.util.CipherFactory;
import com.starter.protomeme.chat.*;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

public class MessageService {

    private final ChatServiceGrpc.ChatServiceStub chatServiceStub;

    private final ChatServiceGrpc.ChatServiceBlockingStub blockingStub;

    private StreamObserver<ChatMessage> requestObserver;

    private StreamObserver<SessionResponse> sessionObserver;

    private StreamObserver<SessionRequest> listenerObserver;

    private DiffieHellman diffieHellman = new DiffieHellman();

    private final SecureRandom random = new SecureRandom();

    public static final ConcurrentHashMap<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public static ConcurrentMap<String, SessionCredentials> credentials = new ConcurrentHashMap<>();

    public static ConcurrentMap<String, CipherContext> sessionCipher = new ConcurrentHashMap<>();

    private DSLContext dsl = DatabaseConnectionManager.getDslContext();

    public MessageService(ManagedChannelBuilder<?> channelBuilder) {
        var channel = channelBuilder.build();
        chatServiceStub = ChatServiceGrpc.newStub(channel);
        blockingStub = ChatServiceGrpc.newBlockingStub(channel);
    }

    public MessageService(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext());
    }

    public void chatStream(Consumer<String> onMessageReceived, Consumer<String> onSessionNotFound, Consumer<String> onFileReceived) {
        requestObserver = chatServiceStub.chatStream(new StreamObserver<>() {
            @Override
            public void onNext(ChatMessage value) {
                if (!value.getSessionId().equals(ChatController.currentSession)) {
                    return;
                }
                if (value.getMessageStatus().equals(MessageStatus.SESSION_NOT_FOUND)) {
                    deleteSession(value.getSessionId());
                    onSessionNotFound.accept(value.getSessionId());
                    return;
                }
                var cipher = sessionCipher.get(value.getSessionId());
                if (cipher == null) {
                    onError(new RuntimeException("нет шифра"));
                    return;
                }
                if (value.getMessageStatus().equals(MessageStatus.TEXT)) {
                    cipher.decryptAsync(value.getMessage().toByteArray())
                            .thenAccept(msg -> onMessageReceived.accept(value.getSender() + ": " + new String(msg)));
                } else if (value.getMessageStatus().equals(MessageStatus.FILE)) {
                    Minio.getFile(value.getSessionId(), value.getMessage().toStringUtf8());
                    System.out.println("Файл получен: " + value.getMessage().toStringUtf8());
                    onFileReceived.accept(value.getMessage().toStringUtf8());
                }


            }

            @Override
            public void onError(Throwable t) {
            }

            @Override
            public void onCompleted() {
                System.out.println("Сервер закрыл соединение.");
            }
        });
    }

    private void deleteSession(String sessionId) {
        sessions.remove(sessionId);
        sessionCipher.remove(sessionId);
        dsl.deleteFrom(DSL.table("sessions"))
                .where(DSL.field("id").eq(sessionId))
                .execute();
    }

    private StreamObserver<SessionResponse> makeSessionObserver(Consumer<SessionInfo> onSessionCreated) {
        return new StreamObserver<>() {
            @Override
            public void onNext(SessionResponse value) {
                if (value.getStatus() == Status.SUCCESS) {
                    final SessionResponse newValue = swapSessionResponse(value);
                    sessions.put(newValue.getSessionId(), newValue.getSessionInfo());
                    var cred = credentials.get(newValue.getSessionId());
                    if (cred == null) {
                        System.out.println("Rec: " + newValue.getSessionInfo().getRecipient());
                        System.out.println("Sen: " + newValue.getSessionInfo().getSender());
                        onError(new RuntimeException("хз че не так"));
                        return;
                    }
                    try {
                        byte[] key = diffieHellman.getKey(cred.getA(), new BigInteger(newValue.getPublicB().toByteArray()), cred.getP());
                        System.out.println(cred.getP().toString() + cred.getG().toString());
                        System.out.println(Arrays.toString(key));
                        CipherContext cipher = CipherFactory.createCipher(newValue.getSessionInfo(), key);
                        System.out.println(newValue.getSessionId());
                        sessionCipher.put(newValue.getSessionId(), cipher);
                        dsl.insertInto(DSL.table("sessions"))
                                .values(UUID.fromString(newValue.getSessionId()),
                                        newValue.getSessionInfo().getSender(),
                                        newValue.getSessionInfo().getRecipient(),
                                        newValue.getSessionInfo().getPadding().name(),
                                        newValue.getSessionInfo().getCipherMode().name(),
                                        newValue.getSessionInfo().getCipher(),
                                        newValue.getSessionInfo().getIv().toByteArray(),
                                        key)
                                .execute();

                        onSessionCreated.accept(newValue.getSessionInfo());

                    } catch (Exception e) {
                        onError(e);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Completed");
            }
        };
    }

    private SessionResponse swapSessionResponse(SessionResponse request) {
        var info = request.getSessionInfo();
        var sessionInfoResponse = SessionInfo.newBuilder()
                .setCipher(request.getSessionInfo().getCipher())
                .setSessionId(request.getSessionId())
                .setCipherMode(info.getCipherMode())
                .setSender(HelloApplication.name)
                .setRecipient(info.getSender())
                .setPadding(info.getPadding())
                .setIv(info.getIv())
                .build();
        return SessionResponse.newBuilder()
                .setSessionId(request.getSessionId())
                .setStatus(Status.SUCCESS)
                .setPublicB(request.getPublicB())
                .setSessionInfo(sessionInfoResponse)
                .build();
    }

    public void makeSession(SessionRequest sessionRequest, Consumer<SessionInfo> onSessionCreated) {
        sessionObserver = makeSessionObserver(onSessionCreated);
        chatServiceStub.makeSession(sessionRequest, sessionObserver);
    }

    public void sendFile(String session, String filename) {
        if (!sessions.containsKey(session)) {
            System.out.println("Session not found");
            return;
        }
        SessionInfo currentSesion = sessions.get(session);
        var cipher = sessionCipher.get(currentSesion.getSessionId());
        if (cipher == null) {
            throw new RuntimeException("нет шифра");
        }
        System.out.println("письмо хочу закинуть");
        System.out.println(currentSesion);
        if (requestObserver != null) {
            System.out.println("Начинаю отправлять");
            ChatMessage message = ChatMessage.newBuilder()
                    .setMessageStatus(MessageStatus.FILE)
                    .setRecipient(currentSesion.getRecipient())
                    .setSender(HelloApplication.name)
                    .setSessionId(currentSesion.getSessionId())
                    .setMessage(ByteString.copyFrom(filename.getBytes()))
                    .build();
            System.out.println("жму некст");
            requestObserver.onNext(message);
        } else {
            System.err.println("Стрим ещё не инициализирован!");
        }
    }

    public void sendMessage(String session, String text) {
        if (!sessions.containsKey(session)) {
            System.out.println("Session not found");
            return;
        }
        SessionInfo currentSesion = sessions.get(session);
        var cipher = sessionCipher.get(currentSesion.getSessionId());
        if (cipher == null) {
            throw new RuntimeException("нет шифра");
        }
        System.out.println("письмо хочу закинуть");
        System.out.println(currentSesion);
        if (requestObserver != null) {
            System.out.println("Начинаю отправлять");
            cipher.encryptAsync(text.getBytes()).thenAccept(msg -> {
                ChatMessage message = ChatMessage.newBuilder()
                        .setMessageStatus(MessageStatus.TEXT)
                        .setRecipient(currentSesion.getRecipient())
                        .setSender(HelloApplication.name)
                        .setSessionId(currentSesion.getSessionId())
                        .setMessage(ByteString.copyFrom(msg))
                        .build();
                System.out.println("жму некст");
                requestObserver.onNext(message);
            });
        } else {
            System.err.println("Стрим ещё не инициализирован!");
        }
    }

    public void sessionListener(Consumer<SessionInfo> onSessionCreated) {
        listenerObserver = new StreamObserver<SessionRequest>() {
            @Override
            public void onNext(SessionRequest value) {
                Platform.runLater(() -> showNotificationPopup(value, onSessionCreated));
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println(this.getClass().getSimpleName() + " закончил выполнене");
            }
        };
        var request = SessionListenerRequest.newBuilder().setUserId(HelloApplication.name).build();
        chatServiceStub.sessionListener(request, listenerObserver);
    }

    public void closeSession(String session, Consumer<String> onCloseSession) {
        deleteSession(session);
        blockingStub.closeSession(CloseSessionRequest.newBuilder().setSessionId(session).build());
        onCloseSession.accept(session);
    }

    private void showNotificationPopup(SessionRequest request, Consumer<SessionInfo> onSessionCreated) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Новый запрос сессии");
        alert.setHeaderText("Пользователь " + request.getFrom() +
                " хочет начать чат");
        alert.setContentText("Сессия: ");
        ButtonType acceptButton = new ButtonType("Принять", ButtonBar.ButtonData.OK_DONE);
        ButtonType rejectButton = new ButtonType("Отклонить", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(acceptButton, rejectButton);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == acceptButton) {
            byte[] iv = new byte[32];
            BigInteger p = new BigInteger(request.getPublicP().toByteArray());
            BigInteger g = new BigInteger(request.getPublicG().toByteArray());
            BigInteger privateA = diffieHellman.generateCloseA(100);
            BigInteger publicA = diffieHellman.generatePublicA(privateA, p, g);
            random.nextBytes(iv);
            var el = getSessionResponse(request, iv, publicA);
            try {
                byte[] key = diffieHellman.getKey(privateA,
                        new BigInteger(request.getPublicA().toByteArray()),
                        new BigInteger(request.getPublicP().toByteArray()));
                System.out.println(Arrays.toString(key));
                CipherContext cipher = CipherFactory.createCipher(el.getSessionInfo(), key);
                System.out.println(el.getSessionId());
                sessionCipher.put(el.getSessionId(), cipher);
                dsl.insertInto(DSL.table("sessions"))
                        .values(UUID.fromString(el.getSessionId()),
                                el.getSessionInfo().getSender(),
                                el.getSessionInfo().getRecipient(),
                                el.getSessionInfo().getPadding().name(),
                                el.getSessionInfo().getCipherMode().name(),
                                el.getSessionInfo().getCipher(),
                                el.getSessionInfo().getIv().toByteArray(),
                                key)
                        .execute();
                onSessionCreated.accept(el.getSessionInfo());
            } catch (Exception e) {
                e.printStackTrace();
            }
            blockingStub.sessionCreated(el);
            sessions.put(el.getSessionId(), el.getSessionInfo());
            System.out.println("Сессия принята");
        } else {
            System.out.println("Сессия отклонена");
        }
    }

    @NotNull
    private SessionResponse getSessionResponse(SessionRequest request, byte[] iv, BigInteger publicA) {
        var sessionInfoResponse = SessionInfo.newBuilder()
                .setCipher(request.getCipher())
                .setSessionId(request.getSessionId())
                .setCipherMode(request.getCipherMode())
                .setSender(request.getTo())
                .setRecipient(request.getFrom())
                .setPadding(request.getPadding())
                .setIv(ByteString.copyFrom(iv))
                .build();
        return SessionResponse.newBuilder()
                .setSessionId(request.getSessionId())
                .setStatus(Status.SUCCESS)
                .setPublicB(ByteString.copyFrom(publicA.toByteArray()))
                .setSessionInfo(sessionInfoResponse)
                .build();
    }

    public void closeStream() {
        if (requestObserver != null) {
            requestObserver.onCompleted();
        }
    }

    public void getSessionMessages(String sessionId, Consumer<String> onMessageReceived, Consumer<String> addFile) {


        chatServiceStub.getSessionMessages(GetSessionsRequest.newBuilder().setSessionId(sessionId).build(), new StreamObserver<ChatMessage>() {

            private final CipherContext cipher = sessionCipher.get(sessionId);

            @Override
            public void onNext(ChatMessage value) {
                if (value.getMessageStatus().equals(MessageStatus.TEXT)) {
                    cipher.decryptAsync(value.getMessage().toByteArray())
                            .thenAccept(msg -> onMessageReceived.accept(value.getSender() + ": " + new String(msg)));
                } else if (value.getMessageStatus().equals(MessageStatus.FILE)) {
                    Minio.getFile(value.getSessionId(), value.getMessage().toStringUtf8())
                            .thenAccept(avoid ->
                                    addFile.accept(value.getMessage().toStringUtf8())
                            );

                }

            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Всё пришло");
            }
        });
    }
}
