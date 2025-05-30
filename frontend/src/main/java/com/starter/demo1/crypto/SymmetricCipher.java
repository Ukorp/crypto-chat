package com.starter.demo1.crypto;

import java.util.List;

/**
 * Интерфейс для симметричного алгоритма шифрования/дешифрования
 */
public interface SymmetricCipher {
    /**
     * Настраивает алгоритм с указанным ключом
     * @param key ключ шифрования
     */
    void init(byte[] key);

    /**
     * Шифрует данные
     * @param data данные для шифрования
     * @return зашифрованные данные
     */
    byte[] encrypt(byte[] data);

    /**
     * Дешифрует данные
     * @param data данные для дешифрования
     * @return расшифрованные данные
     */
    byte[] decrypt(byte[] data);

    /**
     * Возвращает размер блока алгоритма
     * @return размер блока в байтах
     */
    int getBlockSize();
}