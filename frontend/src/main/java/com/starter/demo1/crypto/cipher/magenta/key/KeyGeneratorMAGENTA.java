package com.starter.demo1.crypto.cipher.magenta.key;

import com.starter.demo1.crypto.KeyGenerator;

import java.util.Arrays;
import java.util.List;

public class KeyGeneratorMAGENTA implements KeyGenerator {

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

    private List<byte[]> keys16(byte[] key) {
        byte[] k1 = Arrays.copyOfRange(key, 0, 8);
        byte[] k2 = Arrays.copyOfRange(key, 8, 16);
        return List.of(k1, k1, k2, k2, k1, k1);
    }

    private List<byte[]> keys24(byte[] key) {
        byte[] k1 = Arrays.copyOfRange(key, 0, 8);
        byte[] k2 = Arrays.copyOfRange(key, 8, 16);
        byte[] k3 = Arrays.copyOfRange(key, 16, 24);
        return List.of(k1, k2, k3, k3, k2, k1);
    }

    private List<byte[]> keys32(byte[] key) {
        byte[] k1 = Arrays.copyOfRange(key, 0, 8);
        byte[] k2 = Arrays.copyOfRange(key, 8, 16);
        byte[] k3 = Arrays.copyOfRange(key, 16, 24);
        byte[] k4 = Arrays.copyOfRange(key, 24, 32);
        return List.of(k1, k2, k3, k4, k4, k3, k2, k1);
    }
}
