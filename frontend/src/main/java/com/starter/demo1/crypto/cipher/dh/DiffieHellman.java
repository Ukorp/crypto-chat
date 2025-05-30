package com.starter.demo1.crypto.cipher.dh;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class DiffieHellman {

    private static final SecureRandom random = new SecureRandom();
    private static final int CERTAINTY = 100; // Вероятность простоты (1 - 1/2^CERTAINTY)

    public BigInteger generateSafePrime(int bitLength) {
        BigInteger p;
        do {
            // Генерируем простое число q, затем p = 2q + 1
            BigInteger q = new BigInteger(bitLength - 1, CERTAINTY, random);
            p = q.multiply(BigInteger.TWO).add(BigInteger.ONE);
        } while (!p.isProbablePrime(CERTAINTY));

        return p;
    }

    public BigInteger findPrimitiveRoot(BigInteger p) {
        // p должно быть безопасным простым
        BigInteger q = p.subtract(BigInteger.ONE).divide(BigInteger.TWO);

        for (int candidate = 2; candidate < 100; candidate++) {
            BigInteger g = BigInteger.valueOf(candidate);
            if (!g.modPow(q, p).equals(BigInteger.ONE) &&
                    !g.modPow(BigInteger.TWO, p).equals(BigInteger.ONE)) {
                return g;
            }
        }

        throw new RuntimeException("а нету числа такого");
    }

    public BigInteger generateCloseA(int bitLength) {
        return new BigInteger(bitLength - 1, random);
    }

    public BigInteger generatePublicA(BigInteger a, BigInteger p, BigInteger g) {
        return g.modPow(a, p);
    }

    public byte[] getKey(BigInteger privateA, BigInteger publicB, BigInteger p) throws NoSuchAlgorithmException {
        System.out.println("B: " + publicB.toString());
        System.out.println("P: " + p.toString());
        System.out.println("key: " + publicB.modPow(privateA, p).toString());
        return getSymmetricKey256(publicB.modPow(privateA, p));
    }

    public byte[] getSymmetricKey256(BigInteger k) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        return sha256.digest(k.toByteArray());
    }

}
