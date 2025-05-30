package com.starter.demo1.crypto;

import java.util.List;

public interface KeyGenerator {

    List<byte[]> generateRoundKeys(byte[] key);
}
