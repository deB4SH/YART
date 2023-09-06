package de.b4sh.yart;

import picocli.CommandLine;

public class Runtime {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Templater()).execute(args);
        System.exit(exitCode);
    }

}
