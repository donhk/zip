package com.donhk.config;

import org.apache.commons.cli.*;

import java.io.File;

public class CliValidator {

    private String[] args;
    private Settings settings = new Settings();

    public CliValidator(String[] args) {
        this.args = args;
    }

    public boolean validate() {
        File zipFile;
        File targetDir;

        Options decompressOpt = new Options();
        Options compressOpt = new Options();
        Options helpOpt = new Options();

        Option input = Option.builder("i").longOpt("input").required(false).hasArg(true).desc("Path to file that will be decompressed").build();
        Option output = Option.builder("o").longOpt("output").required(false).hasArg(true).desc("Output directory").build();
        Option create = Option.builder("c").longOpt("create").required(false).hasArg(true).desc("Path to file that will be created").build();
        Option target = Option.builder("t").longOpt("target").required(false).hasArg(true).desc("Target file/directory that will be compressed").build();
        Option help = Option.builder("h").longOpt("help").required(false).desc("Shows this message").build();

        decompressOpt.addOption(input);
        decompressOpt.addOption(output);
        compressOpt.addOption(create);
        compressOpt.addOption(target);
        helpOpt.addOption(help);


        try {
            CommandLineParser parser = new DefaultParser();

            CommandLine cmd = parser.parse(helpOpt, args, true);
            if (cmd.hasOption("h")) {
                System.out.println("-- HELP --");
                HelpFormatter helpFormatter = new HelpFormatter();
                helpFormatter.printHelp(120, "java -jar zipper.jar -i /path/to/file.zip -o /path/to/dir", "decompress", decompressOpt, " ");
                System.out.println();
                helpFormatter.printHelp(120, "java -jar zipper.jar -c /path/to/file.zip -t /path/to/dir", "compress", compressOpt, " ");
                System.out.println();
                helpFormatter.printHelp(120, "java -jar zipper.jar -h", "help", helpOpt, " ");
                System.exit(0);
            }

            cmd = parser.parse(decompressOpt, args, true);
            if (cmd.hasOption("i")) {
                String sourceVal = cmd.getOptionValue("i");
                String targetVal = cmd.getOptionValue("o");
                zipFile = new File(sourceVal);
                targetDir = new File(targetVal);
                if (!zipFile.canRead()) {
                    System.err.println("Can't read input file " + zipFile.getPath());
                    return false;
                }

                if (!targetDir.isDirectory() && !targetDir.mkdirs()) {
                    System.err.println("Can't create output dir " + targetDir.getPath());
                    return false;
                }
                System.out.println("Decompress mode " + sourceVal + " " + targetVal);

                settings.setCompress(false);
                settings.setDecompress(true);
                settings.setSource(zipFile);
                settings.setTarget(targetDir);

                return true;
            }

            cmd = parser.parse(compressOpt, args, false);
            if (cmd.hasOption("c")) {
                String targetVal = cmd.getOptionValue("c");
                String sourceVal = cmd.getOptionValue("t");
                zipFile = new File(sourceVal);
                targetDir = new File(targetVal);
                if (!zipFile.canRead()) {
                    System.err.println("Can't read input file " + zipFile.getPath());
                    return false;
                }

                if (!targetDir.isDirectory() && !targetDir.mkdirs()) {
                    System.err.println("Can't create output dir " + targetDir.getPath());
                    return false;
                }
                System.out.println("Compress mode " + sourceVal + " " + targetVal);

                settings.setCompress(true);
                settings.setDecompress(false);
                settings.setSource(zipFile);
                settings.setTarget(targetDir);

                return true;
            }
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    public Settings getSettings() {
        return settings;
    }
}
