package com.starter.demo1.crypto.cipher.loki97;

import com.starter.demo1.crypto.SymmetricCipher;
import com.starter.demo1.crypto.cipher.loki97.encrypt.EncryptLokiLong;
import com.starter.demo1.crypto.cipher.loki97.feistel.FeistelCipherLoki;
import com.starter.demo1.crypto.cipher.loki97.key.KeyGeneratorLoki;


public class Loki97 implements SymmetricCipher {
    private FeistelCipherLoki cipher;

    private byte[] key;

    public Loki97(byte[] key) {
        init(key);
    }

    public Loki97(String key) {
        init(key.getBytes());
    }

    @Override
    public void init(byte[] key) {
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalArgumentException("key length must be 16/24/32 bytes long");
        }
//        int rounds = key.length == 32 ? 8 : 6;
        this.key = key;
        this.cipher = new FeistelCipherLoki(new KeyGeneratorLoki(), new EncryptLokiLong(), 16);
    }

    @Override
    public byte[] encrypt(byte[] data) {
        return cipher.encryptBlock(data, key);
    }

    @Override
    public byte[] decrypt(byte[] data) {
        return cipher.decryptBlock(data, key);
    }

    @Override
    public int getBlockSize() {
        return 16;
    }
}
