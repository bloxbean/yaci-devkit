package com.bloxbean.cardano.yacicli.util;

public class ConsoleWriter {
    public static void writeLn(String str, Object...args) {
        System.out.println(String.format(str, args));
    }

    public static void writeWithTab(String str) {
        System.out.print("\t" + str);
    }
}
