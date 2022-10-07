package com.bloxbean.cardano.yacicli.commands.general;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.commands.Version;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.info;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.writeLn;

@ShellComponent
public class VersionCommand implements Version.Command{
    private final static String VERSION = "v0.0.3";

    @ShellMethod(value = "Show version info", key = {"version"})
    public void version() {
        writeLn(info(VERSION));
    }
}
