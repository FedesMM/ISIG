package com.isig.lab2.utils;

public class StringUtils {

    public static String format(String s) {
        while(s.startsWith("0")) {
            s = s.substring(1);
        }
        return s.replace("S", " S")
                .replace("N", " N")
                .replace("W", " W")
                .replace("E", " E");
    }
}