package fr.dossierfacile.watermarkcli;

import picocli.CommandLine;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new WatermarkCommand()).execute(args);
        System.exit(exitCode);
    }
}
