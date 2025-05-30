package com.starter.demo1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionCredentials {
    private String from;
    private String to;
    private BigInteger a;
    private BigInteger g;
    private BigInteger p;
}
