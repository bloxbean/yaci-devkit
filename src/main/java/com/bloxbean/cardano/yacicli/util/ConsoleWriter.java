package com.bloxbean.cardano.yacicli.util;

public class ConsoleWriter {
    public static void writeLn(String str, Object...args) {
        System.out.println(String.format(str, args));
    }

    public static String strLn(String str, Object...args) {
        return String.format(str, args) + "\n";
    }

    public static String strLn(String str) {
        return str + "\n";
    }

    public static void writeWithTab(String str) {
        System.out.print("\t" + str);
    }
}
