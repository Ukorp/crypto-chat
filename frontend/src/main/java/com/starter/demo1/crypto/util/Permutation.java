package com.starter.demo1.crypto.util;

import com.starter.demo1.crypto.mode.EncodeOrder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class Permutation {

    public static byte[] permute(byte[] msg, int[] p, byte startBit, EncodeOrder mode) {
        int lenBytes = p.length;
        byte[] result = new byte[lenBytes / 8];
        switch (mode) {
            case EncodeOrder.LITTLE_ENDIAN -> {
                for (int i = lenBytes - 1; i >= 0; --i) {
                    int offset = i / 8;
                    byte elem = getBitLittleEndian(p[i] - startBit, msg);
                    result[offset] = (byte)((result[offset] << 1) & 0xFF | elem);
                }
            }
            case EncodeOrder.BIG_ENDIAN -> {
                for (int i = lenBytes - 1; i >= 0; --i) {
                    int offset = (lenBytes - 1 - i) / 8;
                    byte elem = getBitBigEndian(p[i] - startBit, msg);
                    result[offset] = (byte)((result[offset] << 1) & 0xFF | elem);
                }
            }
            default -> throw new RuntimeException("Неверный режим");
        }
        return result;
    }

    public static byte getBitLittleEndian(int index, byte[] msg) {
        int indexByte = index / 8;
        int indexBit = index % 8;
        byte needByte = msg[indexByte];
        return (byte) ((needByte >>> indexBit) & 1);
    }

    public static void setBitLittleEndian(int index, int bit, byte[] msg) {
        int indexByte = index / 8;
        int indexBit = index % 8;
        byte needByte = msg[indexByte];
        byte savePart = (byte)(((needByte << (8 - indexBit)) & 0xFF) >>> (8 - indexBit));
        msg[indexByte] = (byte) (((((msg[indexByte] >>> (indexBit + 1)) << 1) | bit) << indexBit) | savePart);
    }

    private static byte getBitBigEndian(int index, byte[] msg) {
        int size = msg.length * 8;
        int indexByte = (size - 1 - index) / 8;
        int indexBit = index % 8;
        byte needByte = msg[indexByte];
        return (byte) ((needByte >>> indexBit) & 1);
    }
}
