package com.starter.demo1.crypto;

/**
 * Интерфейс для выполнения шифрующего преобразования
 */
public interface BlockCipher {
    /**
     * Выполняет шифрующее преобразование блока данных
     * @param inputBlock входной блок
     * @param roundKey раундовый ключ
     * @return зашифрованный блок
     * @throws IllegalArgumentException если размеры блоков неверны
     */
    byte[] encryptBlock(byte[] inputBlock, byte[] roundKey) throws IllegalArgumentException;

    /**
     * Выполняет обратное преобразование блока данных
     * @param inputBlock входной блок
     * @param roundKey раундовый ключ
     * @return расшифрованный блок
     * @throws IllegalArgumentException если размеры блоков неверны
     */
    byte[] decryptBlock(byte[] inputBlock, byte[] roundKey) throws IllegalArgumentException;
}