package com.starter.demo1.crypto.cipher.magenta.encrypt;

import com.starter.demo1.crypto.BlockCipher;

import java.util.Arrays;

import static com.starter.demo1.crypto.util.ByteUtil.*;

public class EncryptFunctionMAGENTA implements BlockCipher {

    private int ALPHA = 0x02;

    private int MODULE = 0x165;

    private byte[] S;

    public EncryptFunctionMAGENTA() {
        calculateS();
    }

    public int power(int a, int exponent) {
        int result = 1;
        for (int i = 0; i < exponent; i++) {
            result = multiplyGF(result, a, MODULE, 8);
        }
        return result;
    }

    public void calculateS() {
        if (S == null) {
            S = new byte[256];
        }
        S[0] = 1;
        for (int i = 1; i < 255; ++i) {
            S[i] = (byte) multiplyGF(S[i - 1], ALPHA, MODULE, 8);
        }
        S[255] = 0;
    }

    public boolean isRoot(int alpha) {

        int alpha2 = power(alpha, 2);
        int alpha5 = power(alpha, 5);
        int alpha6 = power(alpha, 6);
        int alpha8 = power(alpha, 8);

        int sum = alpha8 ^ alpha6 ^ alpha5 ^ alpha2 ^ 1;
        return sum == 0;
    }

    public byte f(byte x) {
        return S[Byte.toUnsignedInt(x)];
    }

    public byte a(byte a, byte b) {
        return f((byte) (a ^ f(b)));
    }

    public byte[] pe(byte x, byte y) {
        return new byte[] {a(x, y), a(y, x)};
    }

    public byte[] pi (byte[] b16) {
        if (b16.length != 16) {
            throw new IllegalArgumentException("Массив байт должен быть 16");
        }
        byte[] result = new byte[16];
        for (int i = 0; i < 8; ++i) {
            byte[] tmp = pe(b16[i], b16[i + 8]);
            result[i * 2] = tmp[0];
            result[i * 2 + 1] = tmp[1];
        }
        return result;
    }

    public byte[] t (byte[] b16) {
        return pi(pi(pi(pi(b16))));
    }

    public byte[] cj(byte[] b16, int r) {
        byte[] c = t(b16);
        byte[] ce = new byte[8];
        byte[] co = new byte[8];
        for (int i = 1; i < r; ++i) {
            for (int j = 0; j < 8; ++j) {
                ce[j] = c[j * 2];
                co[j] = c[j * 2 + 1];
            }
            c = t(concatenate(xOrBytes(Arrays.copyOfRange(b16, 0, 8), ce), xOrBytes(Arrays.copyOfRange(b16, 8, 16), co)));
        }
        return c;
    }

    private byte[] e(byte[] b16, int r) {
        byte[] c = cj(b16, r);
        byte[] ce = new byte[8];
        for (int i = 0; i < 8; ++i) {
            ce[i] = c[i * 2];
        }
        return ce;
    }

//    private byte[] fy(byte[] x, byte[] y) {
//        if (x.length != 16) {
//            throw new RuntimeException("x.length != 16");
//        }
//        if (y.length != 8) {
//            throw new RuntimeException("y.length != 16");
//        }
//        byte[] left = Arrays.copyOfRange(x, 8, 16);
//        byte[] right = xOrBytes(Arrays.copyOfRange(x, 0, 8), e(concatenate(Arrays.copyOfRange(x, 8, 16), y), 3));
//        return concatenate(left, right);
//    }
//
//    private byte[] v(byte[] x) {
//        if (x.length != 16) {
//            throw new RuntimeException("x.length != 16");
//        }
//        return concatenate(Arrays.copyOfRange(x, 8, 16), Arrays.copyOfRange(x, 0, 8));
//    }
    
    @Override
    public byte[] encryptBlock(byte[] inputBlock, byte[] roundKey) throws IllegalArgumentException {
        return e(concatenate(inputBlock, roundKey), 3);
    }

    @Override
    public byte[] decryptBlock(byte[] inputBlock, byte[] roundKey) throws IllegalArgumentException {
        return encryptBlock(inputBlock, roundKey);
    }
}
