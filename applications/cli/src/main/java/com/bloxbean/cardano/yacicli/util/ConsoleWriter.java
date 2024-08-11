package com.bloxbean.cardano.yacicli.util;

import com.bloxbean.cardano.yacicli.common.AnsiColors;

public class ConsoleWriter {
    public static void writeLn(String str, Object...args) {
        if (args.length != 0)
            System.out.println(String.format(str, args));
        else
            System.out.println(str);
    }

    public static void write(String str, Object...args) {
        if (args.length != 0)
            System.out.print(String.format(str, args));
        else
            System.out.print(str);
    }

    public static String success(String str, Object...args) {
        String PREFIX = AnsiColors.BLUE + "[Success] " + AnsiColors.ANSI_RESET;
        if (args.length != 0)
            return PREFIX + String.format(str, args);
        else
            return PREFIX + str;
    }

    public static String successLabel(String label, String str, Object...args) {
        String PREFIX = AnsiColors.BLUE + "[" + label + "] " + AnsiColors.ANSI_RESET;
        if (args.length != 0)
            return PREFIX + String.format(str, args);
        else
            return PREFIX + str;
    }

    public static String info(String str, Object...args) {
        String PREFIX = AnsiColors.YELLOW_BRIGHT + "[Info] " + AnsiColors.ANSI_RESET;
        if (args.length != 0)
            return PREFIX + String.format(str, args);
        else
            return PREFIX + str;
    }

    public static String infoLabel(String label, String str, Object...args) {
        String PREFIX = AnsiColors.YELLOW_BRIGHT + "[" + label + "] " + AnsiColors.ANSI_RESET;
        if (args.length != 0)
            return PREFIX + String.format(str, args);
        else
            return PREFIX + str;
    }

    public static String error(String str, Object...args) {
        String PREFIX = AnsiColors.RED + "[ERROR] " + AnsiColors.ANSI_RESET;
        if (args.length != 0)
            return PREFIX + String.format(str, args);
        else
           return PREFIX + str;
    }

    public static String errorLabel(String label, String str, Object...args) {
        String PREFIX = AnsiColors.RED + "[" + label + "] " + AnsiColors.ANSI_RESET;
        if (args.length != 0)
            return PREFIX + String.format(str, args);
        else
            return PREFIX + str;
    }

    public static String warn(String str, Object...args) {
        String PREFIX = AnsiColors.PURPLE + "[WARN] " + AnsiColors.ANSI_RESET;
        if (args.length != 0)
            return PREFIX + String.format(str, args);
        else
            return PREFIX + str;
    }

    public static String warnLabel(String label, String str, Object...args) {
        String PREFIX = AnsiColors.PURPLE + "[" + label + "] " + AnsiColors.ANSI_RESET;
        if (args.length != 0)
            return PREFIX + String.format(str, args);
        else
            return PREFIX + str;
    }

    public static String strLn(String str, Object...args) {
        if (args.length > 0)
            return String.format(str, args) + "\n";
        else
            return str + "\n";
    }

    public static String strLn(String str) {
        return str + "\n";
    }

    public static void writeWithTab(String str) {
        System.out.print("\t" + str);
    }

    public static String header(String color, String str) {
        String text = color + str + AnsiColors.ANSI_RESET;
        return text;
    }
}
