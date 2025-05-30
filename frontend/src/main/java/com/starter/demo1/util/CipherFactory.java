package com.starter.demo1.util;

import com.starter.demo1.crypto.CipherContext;
import com.starter.demo1.crypto.SymmetricCipher;
import com.starter.demo1.crypto.cipher.loki97.Loki97;
import com.starter.demo1.crypto.cipher.magenta.Magenta;
import com.starter.protomeme.chat.CipherMode;
import com.starter.protomeme.chat.Padding;
import com.starter.protomeme.chat.SessionInfo;

import static org.jooq.impl.DSL.field;

public abstract class CipherFactory {
    public static CipherContext createCipher(SessionInfo session, byte[] cipherKey) {
        CipherMode mode = session.getCipherMode();
        Padding padding = session.getPadding();
        byte[] iv = session.getIv().toByteArray();
        SymmetricCipher cipher = switch (session.getCipher()) {
            case "MAGENTA" -> new Magenta(cipherKey);
            case "LOKI97" -> new Loki97(cipherKey);
            default -> throw new IllegalArgumentException("Unknown cipher: " + session.getCipher());
        };
        return new CipherContext(
                cipher,
                mode,
                padding,
                iv,
                null);
    }
}
