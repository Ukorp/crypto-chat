package com.starter.demo1.crypto.util;

import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

@UtilityClass
public class ByteUtil {

    public byte[] xOrBytes(byte[] a, byte[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Lengths of arrays do not match");
        }
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) ((a[i] ^ b[i]) & 0xFF);
        }
        return result;
    }

    public byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public void byteRightShift(byte[] key, int shift) {
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte)(Byte.toUnsignedInt(key[i]) >>> shift);
            if (i != key.length - 1) {
                key[i] |= (byte) (key[i + 1] << (8 - shift));
            }
        }
    }

    public void byteRightShiftBigEndian(byte[] key, int shift) {
        int ost = 0;
        for (int i = 0; i < key.length; i++) {
            int prevOst = ost;
            ost = key[i] & ((1 << shift) - 1);
            key[i] = (byte)(Byte.toUnsignedInt(key[i]) >>> shift);
            key[i] |= (byte) (prevOst << (8 - shift));

        }
    }

    public void byteLeftShift(byte[] key, int shift) {
        int divide = shift / 8;
        int carry = shift % 8;
        int ost = 0;
        if (divide > 0) {
            System.arraycopy(key, divide, key, 0, key.length - divide);
            for (int i = key.length - divide; i < key.length; i++) {
                key[i] = 0;
            }
        }
        for (int i = key.length - 1; i >= 0; i--) {
            int prevOst = ost;
            ost = Byte.toUnsignedInt(key[i]) >>> (8 - carry);
            key[i] <<= carry;
            key[i] |= (byte) (prevOst & 0xFF);

        }
    }

    public void cycleLeftShift(byte[] key, int shift) {
        byte temp = 0;
        for (int i = 0; i < key.length; i++) {
            byte oldKey = key[i];
            key[i] = (byte) ((Byte.toUnsignedInt(key[i]) << shift) & 0xFF);
            key[i] |= temp;
            temp = (byte) (Byte.toUnsignedInt(oldKey) >>> (8 - shift));
        }
        key[0] |= temp;
    }

    public byte[][] splitData(byte[] data, int blockCount) {
        int blockSize = data.length / blockCount;
        byte[][] blocks = new byte[blockCount][];
        for (int i = 0; i < blockCount; i++) {
            int start = i * blockSize;
            int end = Math.min(start + blockSize, data.length);
            blocks[i] = Arrays.copyOfRange(data, start, end);
        }

        return blocks;
    }

    public void incrementBytes(byte[] counter) {
        for (int i = 0; i < counter.length; i++) {
            if (++counter[i] != 0) {
                return;
            }
        }
    }

    public void incrementBytesLittleEndian(byte[] counter, int plus) {
        if (counter.length == 64) {
            long num = bytesToLong(counter) + plus;
            System.arraycopy(longToBytes(num, ByteOrder.LITTLE_ENDIAN), 0, counter, 0, 64);
        }
        else {
            var bytesPlus = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(plus).array();
            var result = addBytesLittleEndian(bytesPlus, counter);
            System.arraycopy(result, 0, counter, 0, counter.length);
        }

    }

    public byte[] addBytesLittleEndian(byte[] a, byte[] b) {
        if (a.length < b.length) {
            a = Arrays.copyOf(a, b.length);
        } else if (b.length < a.length) {
            b = Arrays.copyOf(b, a.length);
        }
        byte[] result = new byte[a.length];
        int carry = 0;
        for (int i = 0; i < a.length; i++) {
            int sum = (a[i] & 0xFF) + (b[i] & 0xFF) + carry;
            result[i] = (byte) (sum & 0xFF);
            carry = sum >>> 8;
        }

        return result;
    }

    public byte[] addBytesLittleEndian(byte[] a, long number) {
        var b = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(number).array();
        return addBytesLittleEndian(a, b);
    }

    public int multiplyGF(int a, int b, int module, int n) {
        a &= (1 << n) - 1;
        b &= (1 << n) - 1;

        int result = 0;
        for (int i = 0; i < n; i++) {
            if ((b & 1) != 0) {
                result ^= a;
            }
            boolean carry = (a & (1 << (n - 1))) != 0;
            a <<= 1;
            if (carry) {
                a ^= module;
            }
            b >>= 1;
        }
        return result;
    }

    public int power(int a, int exponent, int mod, int n) {
        int result = 1;
        for (int i = 0; i < exponent; i++) {
            result = (int) multiplyGF(result, a, mod, n);
        }
        return result;
    }

    public byte[] getBits(byte[] bytes, int n) {
        int size = (n + 7) / 8;
        int carry = n % 8;
        byte[] res = new byte[size];
        System.arraycopy(bytes, 0, res, 0, size);
        if (carry > 0) {
            res[size - 1] = (byte) (res[size - 1] >>> (8 - carry) << (8 - carry));
        }
        return res;
    }

//    public static void main(String[] args) {
//        long num = 0x0102030405060708L;
//        var k = longToBytes(num);
//        for (int i = 0; i < 8; ++i) {
//            System.out.println(Long.toHexString(bytesToLong(getBits(k, 13))));
//            byteLeftShift(k, 13);
//        }
//    }

    public byte[] getBitsBigEndian(byte[] bytes, int n) {
        int size = (n + 7) / 8;
        int carry = n % 8;
        byte[] res = Arrays.copyOfRange(bytes, bytes.length - size, bytes.length);
        if (carry > 0) {
            res[0] &= (byte) ((1 << carry) - 1);
        }
        return res;
    }

    public byte[] longToBytes(long val) {
        return ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN).putLong(val).array();
    }

    public long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getLong();
    }

    public byte[] longToBytes(long val, ByteOrder byteOrder) {
        return ByteBuffer.allocate(Long.BYTES).order(byteOrder).putLong(val).array();
    }

    public long bytesToLong(byte[] bytes, ByteOrder byteOrder) {
        return ByteBuffer.wrap(bytes).order(byteOrder).getLong();
    }

//    public void main(String[] args) {
//        var bytes = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
//                .put((byte) 0xFF)
//                .put((byte) 0x1F)
//                        .array();
//        System.out.println(Arrays.toString(bytes));
//        System.out.println(Arrays.toString(getBits(bytes, 12)));
//    }
}
