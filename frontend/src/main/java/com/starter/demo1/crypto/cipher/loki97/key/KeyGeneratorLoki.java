package com.starter.demo1.crypto.cipher.loki97.key;



import com.starter.demo1.crypto.BlockCipher;
import com.starter.demo1.crypto.KeyGenerator;
import com.starter.demo1.crypto.cipher.loki97.encrypt.EncryptLokiLong;
import com.starter.demo1.crypto.util.ByteUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class KeyGeneratorLoki implements KeyGenerator {

    private final BlockCipher cipher = new EncryptLokiLong();

    private static final long DELTA = 0x9E3779B97F4A7C15L;

    private long g(long k1, long k3, long k2, long i) {
        byte[] a = longToBytes(k1 + k3 + DELTA * i);

        byte[] b = longToBytes(k2);
        byte[] res = cipher.encryptBlock(a, b);
        return bytesToLong(res);
    }

    @Override
    public List<byte[]> generateRoundKeys(byte[] key) {
        return switch (key.length) {
            case 16 -> keys16(key);
            case 24 -> keys24(key);
            case 32 -> keys32(key);
            default -> throw new IllegalArgumentException(
                    String.format("Key must be 16/24/32 bytes long (having %d bytes)", key.length)
            );
        };
    }

    private List<byte[]> keys32(byte[] key) {
        long k1 = ByteBuffer.wrap(Arrays.copyOfRange(key, 0, 8)).getLong();
        long k2 = ByteBuffer.wrap(Arrays.copyOfRange(key, 8, 16)).getLong();
        long k3 = ByteBuffer.wrap(Arrays.copyOfRange(key, 16, 24)).getLong();
        long k4 = ByteBuffer.wrap(Arrays.copyOfRange(key, 24, 32)).getLong();

        return keys32(k4, k3, k2, k1);
    }

    private List<byte[]> keys32(long k4, long k3, long k2, long k1) {
        List<byte[]> SK = new ArrayList<>();
        for (int i = 1; i <= 48; ++i) {
            long tmp = k4 ^ g(k1, k3, k2, i);
            SK.add(ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN).putLong(tmp).array());
            k4 = k3;
            k3 = k2;
            k2 = k1;
            k1 = tmp;
        }
        return SK;
    }

    private static byte[] longToBytes(long val) {
        return ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN).putLong(val).array();
    }

    private static long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    private List<byte[]> keys24(byte[] key) {
        long k1 = ByteBuffer.wrap(Arrays.copyOfRange(key, 0, 8)).getLong();
        var k2 = ByteBuffer.wrap(Arrays.copyOfRange(key, 8, 16));
        var k3 = ByteBuffer.wrap(Arrays.copyOfRange(key, 16, 24));
        long k4 = ByteUtil.bytesToLong(cipher.encryptBlock(k2.array(), k3.array()));
        return keys32(k4, k3.getLong(), k2.getLong(), k1);
    }

    private List<byte[]> keys16(byte[] key) {
        var k1 = ByteBuffer.wrap(Arrays.copyOfRange(key, 0, 8));
        var k2 = ByteBuffer.wrap(Arrays.copyOfRange(key, 8, 16));
        long k3 = ByteUtil.bytesToLong(cipher.encryptBlock(k2.array(), k1.array()));
        long k4 = ByteUtil.bytesToLong(cipher.encryptBlock(k1.array(), k2.array()));
        return keys32(k4, k3, k2.getLong(), k1.getLong());
    }
}
