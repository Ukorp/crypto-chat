package com.starter.demo1.crypto;

import com.starter.demo1.crypto.util.ByteUtil;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.List;

public class FeistelCipher implements BlockCipher {
    protected final KeyGenerator keyGenerator;
    protected final BlockCipher roundCipher;
    protected final int rounds;

    /**
     * @param keyGenerator реализация интерфейса 2.1 (генерация раундовых ключей)
     * @param roundCipher  реализация интерфейса 2.2 (шифрующее преобразование)
     * @param rounds       количество раундов
     */
    public FeistelCipher(KeyGenerator keyGenerator,
                         BlockCipher roundCipher,
                         int rounds) {
        this.keyGenerator = keyGenerator;
        this.roundCipher = roundCipher;
        this.rounds = rounds;
    }

    @Override
    public byte[] encryptBlock(byte[] inputBlock, byte[] key) throws IllegalArgumentException {
        List<byte[]> keys = keyGenerator.generateRoundKeys(key);
        int half = inputBlock.length / 2;
        byte[] left = Arrays.copyOfRange(inputBlock, 0, half);
        byte[] right = Arrays.copyOfRange(inputBlock, half, inputBlock.length);
        for (int i = 0; i < rounds; i++) {
            byte[] tmp = right;
            right = ByteUtil.xOrBytes(left, roundCipher.encryptBlock(right, keys.get(i)));
            left = tmp;
        }
        return ByteUtil.concatenate(right, left);
    }

    @Override
    @SneakyThrows
    public byte[] decryptBlock(byte[] inputBlock, byte[] key) throws IllegalArgumentException {
        List<byte[]> keys = keyGenerator.generateRoundKeys(key);
        int half = inputBlock.length / 2;
        byte[] left = Arrays.copyOfRange(inputBlock, 0, half);
        byte[] right = Arrays.copyOfRange(inputBlock, half, inputBlock.length);
        for (int i = rounds - 1; i >= 0; --i) {
            byte[] tmp = right;
            right = ByteUtil.xOrBytes(left, roundCipher.encryptBlock(right, keys.get(i)));
            left = tmp;
        }
        return ByteUtil.concatenate(right, left);
    }
}