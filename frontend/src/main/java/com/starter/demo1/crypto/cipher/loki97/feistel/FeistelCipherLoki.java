package com.starter.demo1.crypto.cipher.loki97.feistel;

import com.starter.demo1.crypto.BlockCipher;
import com.starter.demo1.crypto.FeistelCipher;
import com.starter.demo1.crypto.KeyGenerator;
import com.starter.demo1.crypto.util.ByteUtil;
import lombok.SneakyThrows;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

public class FeistelCipherLoki extends FeistelCipher {
    /**
     * @param keyGenerator реализация интерфейса 2.1 (генерация раундовых ключей)
     * @param roundCipher  реализация интерфейса 2.2 (шифрующее преобразование)
     * @param rounds       количество раундов
     */
    public FeistelCipherLoki(KeyGenerator keyGenerator, BlockCipher roundCipher, int rounds) {
        super(keyGenerator, roundCipher, rounds);
    }

    @Override
    public byte[] encryptBlock(byte[] inputBlock, byte[] key) throws IllegalArgumentException {
        List<Long> keys = getLongs(key);
        int half = inputBlock.length / 2;
        long left = ByteBuffer.wrap(Arrays.copyOfRange(inputBlock, 0, half)).order(ByteOrder.BIG_ENDIAN).getLong();
        long right = ByteBuffer.wrap(Arrays.copyOfRange(inputBlock, half, inputBlock.length)).order(ByteOrder.BIG_ENDIAN).getLong();
        for (int i = 1; i <= rounds; i++) {
            long rightPrev = right;
            right = left ^ encryptLong(right + keys.get(3 * i - 2 - 1), keys.get(3 * i - 1 - 1));
            left = rightPrev + keys.get(3 * i - 2 - 1) + keys.get(3 * i - 1);
        }
        return ByteBuffer.allocate(Long.BYTES * 2).order(ByteOrder.BIG_ENDIAN).putLong(right).putLong(left).array();
    }

    private List<Long> getLongs(byte[] key) {
        return keyGenerator
                .generateRoundKeys(key)
                .stream()
                .map(elem -> ByteBuffer.wrap(elem).order(ByteOrder.BIG_ENDIAN).getLong())
                .toList();
    }

    private long encryptLong(long a, long b) {
        var result = roundCipher.encryptBlock(
                ByteUtil.longToBytes(a),
                ByteUtil.longToBytes(b));
        return ByteUtil.bytesToLong(result);
    }

    @Override
    @SneakyThrows
    public byte[] decryptBlock(byte[] inputBlock, byte[] key) throws IllegalArgumentException {
        List<Long> keys = getLongs(key);
        int half = inputBlock.length / 2;
        long right = ByteBuffer.wrap(Arrays.copyOfRange(inputBlock, 0, half)).order(ByteOrder.BIG_ENDIAN).getLong();
        long left = ByteBuffer.wrap(Arrays.copyOfRange(inputBlock, half, inputBlock.length)).order(ByteOrder.BIG_ENDIAN).getLong();
        for (int i = rounds; i >= 1; i--) {
            long leftPrev = left;
            left = right ^ encryptLong(left - keys.get(3 * i - 1), keys.get(3 * i - 1 - 1));
            right = leftPrev - keys.get(3 * i - 1) - keys.get(3 * i - 2 - 1);
        }
        return ByteBuffer.allocate(Long.BYTES * 2).order(ByteOrder.BIG_ENDIAN).putLong(left).putLong(right).array();
    }

}
