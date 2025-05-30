package com.starter.demo1.crypto.cipher.loki97.encrypt;


import com.starter.demo1.crypto.BlockCipher;
import com.starter.demo1.crypto.mode.EncodeOrder;

import static com.starter.demo1.crypto.util.ByteUtil.*;
import static com.starter.demo1.crypto.util.Permutation.permute;

public class EncryptLokiLong implements BlockCipher {

    protected static final int[] P = {
            56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1,
            58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35, 27, 19, 11, 3,
            60, 52, 44, 36, 28, 20, 12, 4, 61, 53, 45, 37, 29, 21, 13, 5,
            62, 54, 46, 38, 30, 22, 14, 6, 63, 55, 47, 39, 31, 23, 15, 7
    };

    private long kp(long a, long b) {
        int l = (int) (a >>> 32);
        int r = (int) (a & 0xFFFFFFFFL);
        int skr = (int) (b & 0xFFFFFFFFL);

        long first = Integer.toUnsignedLong((l & (~skr)) | (r & skr)) << 32;
        long second = Integer.toUnsignedLong((r & (~skr)) | (l & skr));
        return first | second;
    }

    private int[] e(long a) {
        int[] result = new int[8];
        result[0] = (int)(((a & 0b11111) << 8) | (a >>> 56));
        result[1] = (int) ((a >>> 48) & 0b11111111111);
        result[2] = (int) ((a >>> 40) & 0b1111111111111);
        result[3] = (int) ((a >>> 32) & 0b11111111111);
        result[4] = (int) ((a >>> 24) & 0b11111111111);
        result[5] = (int) ((a >>> 16) & 0b1111111111111);
        result[6] = (int) ((a >>> 8)  & 0b11111111111);
        result[7] = (int)          (a & 0b1111111111111);
        return result;
    }

    private byte s1(int a) {
        a = (~a) & (0x1FFF);
        int afterXor = (a ^ 0x1FFF);
        return (byte) (power(afterXor, 3, 0x2911, 13) & 0xFF);
    }

    private byte s2(int a) {
        a = (~a) & 0x7FF;
        int afterXor = (a ^ 0x7FF);
        return (byte) (power(afterXor, 3, 0xAA7, 11) & 0xFF);
    }

    private long p(long arr) {
        return bytesToLong(permute(longToBytes(arr), P, (byte) 0, EncodeOrder.BIG_ENDIAN));
    }

    private long sa(int[] a) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= switch (i) {
                case 0, 2, 5, 7 -> Byte.toUnsignedLong(s1(a[i]));
                case 1, 3, 4, 6 -> Byte.toUnsignedLong(s2(a[i]));
                default -> throw new IllegalStateException("Unexpected value: " + i);
            };
        }
        return result;
    }

    private long sb(long a, long b) {
        int bl = (int) (b >>> 32);
        int[] p = new int[8];
        p[0] = (bl >>> 21) & 0b11111111111;
        p[1] = (bl >>> 10) & 0b11111111111;
        p[2] = (bl & 0b1111111111) << 3 | (int) (a >>> 61);
        p[3] = (int) ((a >>> 48) & 0b1111111111111);
        p[4] = (int) ((a >>> 37) & 0b11111111111);
        p[5] = (int) ((a >>> 26) & 0b11111111111);
        p[6] = (int) ((a >>> 13) & 0b1111111111111);
        p[7] = (int) (a & 0b1111111111111);
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= switch (i) {
                case 0, 1, 4, 5 -> Byte.toUnsignedLong(s2(p[i]));
                case 2, 3, 6, 7 -> Byte.toUnsignedLong(s1(p[i]));
                default -> throw new IllegalStateException("Unexpected value: " + i);
            };
        }
        return result;
    }

    private long encryptBlockLong(long inputBlock, long roundKey) {
        return sb(p(sa(e(kp(inputBlock, roundKey)))), roundKey);
    }


    @Override
    public byte[] encryptBlock(byte[] inputBlock, byte[] roundKey) throws IllegalArgumentException {
        long a = bytesToLong(inputBlock);
        long b = bytesToLong(roundKey);
        return longToBytes(encryptBlockLong(a, b));
    }

    @Override
    public byte[] decryptBlock(byte[] inputBlock, byte[] roundKey) throws IllegalArgumentException {
        return encryptBlock(inputBlock, roundKey);
    }

}
