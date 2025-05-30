package com.starter.demo1.crypto;

import com.starter.demo1.crypto.cipher.loki97.Loki97;
import com.starter.demo1.crypto.util.ByteUtil;
import com.starter.protomeme.chat.CipherMode;
import com.starter.protomeme.chat.Padding;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;


@Slf4j
public class CipherContext {

    private static final int BLOCK_COUNT = 1024;

    @Getter
    @Setter
    private SymmetricCipher cipher;

    @Getter
    @Setter
    private CipherMode mode;

    @Getter
    @Setter
    private Padding padding;

    @Getter
    @Setter
    private byte[] iv;

    @Getter
    @Setter
    private Object[] additionalParams;

    private final ForkJoinPool executor = new ForkJoinPool();

    /**
     * Конструктор контекста
     *
     * @param cipher           реализация алгоритма
     * @param mode             режим шифрования
     * @param padding          режим дополнения
     * @param iv               вектор инициализации (может быть null)
     * @param additionalParams дополнительные параметры
     */
    public CipherContext(SymmetricCipher cipher, CipherMode mode, Padding padding,
                             byte[] iv, Object... additionalParams) {
        this.cipher = cipher;
        this.mode = mode;
        this.padding = padding;
        this.iv = iv != null ? iv.clone() : null;
        this.additionalParams = additionalParams;
    }

    public CompletableFuture<byte[]> encryptAsync(byte[] data) {
        return CompletableFuture.supplyAsync(() -> processData(data, true), executor);
    }

    public CompletableFuture<byte[]> decryptAsync(byte[] data) {
        return CompletableFuture.supplyAsync(() -> processData(data, false), executor);
    }

    public CompletableFuture<Void> encryptFileAsync(Path inputFile, Path outputFile) {
        return processFileAsync(inputFile, outputFile, true);
    }

    public CompletableFuture<Void> decryptFileAsync(Path inputFile, Path outputFile) {
        return processFileAsync(inputFile, outputFile, false);
    }

    private CompletableFuture<Void> processFileAsync(Path inputFile, Path outputFile, boolean encrypt) {
        return CompletableFuture.runAsync(() -> {
            byte[] buffer = new byte[cipher.getBlockSize() * BLOCK_COUNT];
            int bytesRead;
            try {
                Files.deleteIfExists(outputFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            try (InputStream inputStream = Files.newInputStream(inputFile);
                 OutputStream outputStream = new FileOutputStream(outputFile.toFile(), true)) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    if (bytesRead != buffer.length) {
                        outputStream.write(processData(Arrays.copyOf(buffer, bytesRead), encrypt));
                    } else {
                        outputStream.write(processData(buffer, encrypt));
                    }
                }
                outputStream.flush();
            } catch (Exception e) {
                throw new RuntimeException("File processing failed", e);
            }
        });
    }

    private byte[] processData(byte[] data, boolean encrypt) {
        byte[][] blocks = splitIntoBlocks(data);
        if (encrypt) {
            blocks = fillBlocksWithPadding(blocks);
        }
        blocks = encryptWithMode(blocks, encrypt);
        if (!encrypt) {
            blocks = unfillBlocksWithPadding(blocks);
        }
        return joinBlocks(blocks);
    }

    private byte[][] splitIntoBlocks(byte[] data) {
        int blockSize = cipher.getBlockSize();
        int blockCount = (data.length + blockSize - 1) / blockSize;
        byte[][] blocks = new byte[blockCount][];

        for (int i = 0; i < blockCount; i++) {
            int start = i * blockSize;
            int end = Math.min(start + blockSize, data.length);
            blocks[i] = Arrays.copyOfRange(data, start, end);
        }

        return blocks;
    }

    private byte[] joinBlocks(byte[][] blocks) {
        int totalLength = Arrays.stream(blocks).mapToInt(b -> b.length).sum();
        byte[] result = new byte[totalLength];
        int offset = 0;

        for (byte[] block : blocks) {
            System.arraycopy(block, 0, result, offset, block.length);
            offset += block.length;
        }
        return result;
    }

    private byte[][] fillBlocksWithPadding(byte[][] blocks) {
        blocks = blocks.clone();
        return switch (padding) {
            case ZEROS -> zerosFill(blocks);
            case ANSIX923 -> ansixFill(blocks);
            case ISO10126 -> isoFill(blocks);
            case PKCS7 -> pkcs7Fill(blocks);
            default -> throw new RuntimeException("Padding mode not supported");
        };
    }

    private byte[][] pkcs7Fill(byte[][] blocks) {
        blocks = ansixFill(blocks);
        int size = blocks.length * cipher.getBlockSize();
        byte additionalBlocks = getLast(blocks);
        for (int i = size - 2; i >= size - additionalBlocks; --i) {
            setByIndex(blocks, i, additionalBlocks);
        }
        return blocks;
    }

    private byte[][] isoFill(byte[][] blocks) {
        blocks = ansixFill(blocks);
        int size = blocks.length * cipher.getBlockSize();
        byte additionalBlocks = getLast(blocks);
        for (int i = size - 2; i >= size - additionalBlocks; --i) {
            setByIndex(blocks, i, (byte) (ThreadLocalRandom.current().nextInt() & 0xFF));
        }
        return blocks;
    }

    public static byte getLast(byte[][] blocks) {
        return getLast(blocks[blocks.length - 1]);
    }

    public static byte getLast(byte[] blocks) {
        return blocks[blocks.length - 1];
    }

    public byte getByIndex(byte[][] blocks, int index) {
        int blocksCount = (index + cipher.getBlockSize() - 1) / cipher.getBlockSize();
        int bytesInBlock = index % cipher.getBlockSize();
        return blocks[blocksCount][bytesInBlock];
    }

    public void setByIndex(byte[][] blocks, int index, byte value) {
        int blocksCount = index / cipher.getBlockSize();
        int bytesInBlock = index % cipher.getBlockSize();
        blocks[blocksCount][bytesInBlock] = value;
    }

    private byte[][] ansixFill(byte[][] blocks) {
        if (blocks[blocks.length - 1].length < cipher.getBlockSize()) {
            int prevSize = blocks[blocks.length - 1].length;
            blocks[blocks.length - 1] = Arrays.copyOf(blocks[blocks.length - 1], cipher.getBlockSize());
            blocks[blocks.length - 1][cipher.getBlockSize() - 1] = (byte) (cipher.getBlockSize() - prevSize);
        } else {
            var newBlock = new byte[blocks.length + 1][cipher.getBlockSize()];
            System.arraycopy(blocks, 0, newBlock, 0, blocks.length);
            blocks = newBlock;
            blocks[blocks.length - 1][cipher.getBlockSize() - 1] = (byte) cipher.getBlockSize();
        }
        return blocks;
    }

    private byte[][] zerosFill(byte[][] blocks) {

        if (blocks[blocks.length - 1].length < cipher.getBlockSize()) {
            byte[] paddedBlock = new byte[cipher.getBlockSize()];
            System.arraycopy(blocks[blocks.length - 1], 0, paddedBlock, 0, blocks[blocks.length - 1].length);
            blocks[blocks.length - 1] = paddedBlock;
        }
        return blocks;
    }

    private byte[][] unfillBlocksWithPadding(byte[][] blocks) {
        blocks = blocks.clone();
        int bytesCount = blocks.length * cipher.getBlockSize();
        bytesCount = switch (padding) {
            case ZEROS -> zerosUnfill(blocks, bytesCount);
            case ANSIX923, ISO10126, PKCS7 -> blocks.length * cipher.getBlockSize() - getLast(blocks);
            default -> throw new RuntimeException("Padding mode not supported");
        };
        int blocksCount = (bytesCount + cipher.getBlockSize() - 1) / cipher.getBlockSize();
        int bytesInBlock = bytesCount % cipher.getBlockSize();
        blocks = Arrays.copyOf(blocks, blocksCount);
        if (bytesInBlock != 0) {
            blocks[blocks.length - 1] = Arrays.copyOf(blocks[blocks.length - 1], bytesInBlock);
        }
        return blocks;
    }

    private int zerosUnfill(byte[][] blocks, int bytesCount) {
        boolean stop = false;
        for (int i = blocks.length - 1; i >= 0; i--) {
            if (stop) {
                break;
            }
            for (int j = blocks[i].length - 1; j >= 0; j--) {
                if (blocks[i][j] != 0) {
                    bytesCount = i * cipher.getBlockSize() + j + 1;
                    stop = true;
                    break;
                }
            }
        }
        return bytesCount;
    }

    private byte[][] encryptWithMode(byte[][] blocks, boolean encrypt) {
        blocks = blocks.clone();
        switch (mode) {
            case ECB -> processECB(blocks, encrypt);
            case CBC -> processCBC(blocks, encrypt);
            case CFB -> processCFB(blocks, encrypt);
            case OFB -> processOFB(blocks);
            case PCBC -> processPCBC(blocks, encrypt);
            case CTR -> processCTR(blocks);
            case RD -> processRD(blocks, encrypt);
            default -> throw new RuntimeException("Mode not supported");
        }
        return blocks;
    }

    private void processOFB(byte[][] blocks) {
        byte[] currentIV = iv.clone();
        for (int i = 0; i < blocks.length; i++) {
            currentIV = cipher.encrypt(currentIV);
            blocks[i] = ByteUtil.xOrBytes(currentIV, blocks[i]);
        }
    }

    private void processCFB(byte[][] blocks, boolean encrypt) {
        byte[][] deepCopy = new byte[blocks.length][];
        for (int i = 0; i < blocks.length; i++) {
            deepCopy[i] = Arrays.copyOf(blocks[i], blocks[i].length);
        }
        if (encrypt) {
            for (int i = 0; i < blocks.length; i++) {
                var currentIV = (i == 0) ? iv.clone() : blocks[i - 1];
                var tmp = cipher.encrypt(currentIV);
                currentIV = ByteUtil.xOrBytes(blocks[i], tmp);
                blocks[i] = currentIV;
            }
        } else {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < blocks.length; i++) {
                final int k = i;
                var future = CompletableFuture.runAsync(() -> {
                    var currentIV = (k == 0) ? iv.clone() : deepCopy[k - 1];
                    var tmp = cipher.encrypt(currentIV);
                    blocks[k] = ByteUtil.xOrBytes(blocks[k], tmp);
                }, executor);
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }

    private void processCBC(byte[][] blocks, boolean encrypt) {
        byte[][] deepCopy = new byte[blocks.length][];
        for (int i = 0; i < blocks.length; i++) {
            deepCopy[i] = Arrays.copyOf(blocks[i], blocks[i].length);
        }
        if (encrypt) {
            for (int i = 0; i < blocks.length; i++) {
                var currentIV = (i == 0) ? iv.clone() : blocks[i - 1];
                var tmp = ByteUtil.xOrBytes(blocks[i], currentIV);
                currentIV = cipher.encrypt(tmp);
                blocks[i] = currentIV;
            }
        } else {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (int i = 0; i < blocks.length; i++) {
                final int k = i;
                var future = CompletableFuture.runAsync(() -> {
                    byte[] tmp = (k == 0) ? iv : deepCopy[k - 1];
                    blocks[k] = ByteUtil.xOrBytes(tmp, cipher.decrypt(blocks[k]));
                }, executor);
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }
    }

    private void processPCBC(byte[][] blocks, boolean encrypt) {
        byte[] currentIV = iv.clone();
        if (encrypt) {
            for (int i = 0; i < blocks.length; i++) {
                var tmp = ByteUtil.xOrBytes(blocks[i], currentIV);
                currentIV = cipher.encrypt(tmp);
                var prev = blocks[i];
                blocks[i] = currentIV;
                currentIV = ByteUtil.xOrBytes(prev, currentIV);
            }
        } else {
            for (int i = 0; i < blocks.length; i++) {
                var prev = blocks[i];
                blocks[i] = ByteUtil.xOrBytes(currentIV, cipher.decrypt(blocks[i]));
                currentIV = ByteUtil.xOrBytes(prev, blocks[i]);
            }
        }
    }

    private void processECB(byte[][] blocks, boolean encrypt) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < blocks.length; i++) {
            final int k = i;
            var future = CompletableFuture.runAsync(() -> blocks[k] = encrypt ? cipher.encrypt(blocks[k]) : cipher.decrypt(blocks[k]), executor);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    private void processCTR(byte[][] blocks) {
        int length = iv.length / 2;
        byte[] arr = new byte[length];
        var start = ByteUtil.concatenate(Arrays.copyOf(iv, length), arr);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < blocks.length; i++) {
            final int k = i;
            var future = CompletableFuture.runAsync(() -> {
                var counter = Arrays.copyOf(start, start.length);
                ByteUtil.incrementBytesLittleEndian(counter, k);
                blocks[k] = ByteUtil.xOrBytes(cipher.encrypt(counter), blocks[k]);
            }, executor);
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    }

    private void processRD(byte[][] blocks, boolean encrypt) {
        final BigInteger bigDelta = new BigInteger(ByteUtil.splitData(iv, 2)[1]);
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        if (encrypt) {
            for (int i = 0; i < blocks.length; i++) {
                final int k = i;
                var future = CompletableFuture.runAsync(() -> {
                    var elem = ByteUtil.addBytesLittleEndian(iv, bigDelta.multiply(BigInteger.valueOf(k + 1L)).toByteArray());
                    blocks[k] = cipher.encrypt(ByteUtil.xOrBytes(elem, blocks[k]));
                });
                futures.add(future);
            }
        } else {
            for (int i = 0; i < blocks.length; i++) {
                final int k = i;
                var future = CompletableFuture.runAsync(() -> {
                    var elem = ByteUtil.addBytesLittleEndian(iv, bigDelta.multiply(BigInteger.valueOf(k + 1L)).toByteArray());
                    blocks[k] = ByteUtil.xOrBytes(cipher.decrypt(blocks[k]), elem);
                });
                futures.add(future);
            }
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    }

    @SneakyThrows
    public static void main(String[] args) {
        String path = "src/main/java/com/test/";
        byte[] bytesLittleEndian = ByteBuffer.allocate(16)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(0x62738493746384FEL)
                .putLong(0x62738493746384FEL)
                .array();
        CipherContext cp = new CipherContext(new Loki97("11111111111111111111jhguuiljuhtt"), CipherMode.ECB, Padding.ZEROS, bytesLittleEndian, (Object) null);
//        cp.encryptFileAsync(Path.of(path + "test.jpg"), Path.of(path + "encrypted.jpg")).get();
//        cp.decryptFileAsync(Path.of(path + "encrypted.jpg"), Path.of(path + "final.jpg")).get();

        String testString = "Dear diary, if you know how much pain I got during I've been writing DES...";
        var encr = cp.encryptAsync(testString.getBytes()).join();
        var decr = cp.decryptAsync(encr).join();
        System.out.println(new String(testString.getBytes()));
        System.out.println(new String(encr));
        System.out.println(new String(decr));
    }
}