package com.starter.demo1.crypto.cipher.magenta;

import com.starter.demo1.crypto.FeistelCipher;
import com.starter.demo1.crypto.SymmetricCipher;
import com.starter.demo1.crypto.cipher.magenta.encrypt.EncryptFunctionMAGENTA;
import com.starter.demo1.crypto.cipher.magenta.key.KeyGeneratorMAGENTA;

import java.util.Arrays;

import  static com.starter.demo1.crypto.util.ByteUtil.*;

public class Magenta implements SymmetricCipher {

    private FeistelCipher cipher;

    private byte[] key;

    public Magenta(byte[] key) {
        init(key);
    }

    public Magenta(String key) {
        init(key.getBytes());
    }

    @Override
    public void init(byte[] key) {
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalArgumentException("key length must be 16/24/32 bytes long");
        }
        int rounds = key.length == 32 ? 8 : 6;
        this.key = key;
        this.cipher = new FeistelCipher(new KeyGeneratorMAGENTA(), new EncryptFunctionMAGENTA(), rounds);
    }

    @Override
    public byte[] encrypt(byte[] data) {
        var cipherText = cipher.encryptBlock(data, key);
        var result = splitData(cipherText, 2);
        return concatenate(result[1], result[0]);
    }

    public static byte[] v(byte[] x) {
        if (x.length != 16) {
            throw new RuntimeException("x.length != 16");
        }
        return concatenate(Arrays.copyOfRange(x, 8, 16), Arrays.copyOfRange(x, 0, 8));
    }

    @Override
    public byte[] decrypt(byte[] data) {
        return v(encrypt(v(data)));
    }

    @Override
    public int getBlockSize() {
        return 16;
    }
}
