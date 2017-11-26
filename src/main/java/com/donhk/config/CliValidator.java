package com.donhk.config;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;

public class CliValidator {

    private String[] args;
    private Settings settings = new Settings();
    private Multimap<String, Long> allowedFileTypes = ArrayListMultimap.create();

    public CliValidator(String[] args) {
        this.args = args;
        allowedFileTypes.put("zip", 0x504B0304L);
        allowedFileTypes.put("zip", 0x504B0506L);
        allowedFileTypes.put("zip", 0x504B0708L);
        allowedFileTypes.put("7z", 0x377ABCAF271CL);
        allowedFileTypes.put("tar", 0x1F8BL); //gzip
        allowedFileTypes.put("tar.gz", 0x1F8BL); //gzip
        allowedFileTypes.put("bz2", 0x425A68L);//bzip2
        allowedFileTypes.put("z", 0x1F9DL);//using Lempel-Ziv-Welch algorithm
        allowedFileTypes.put("tar.z", 0x1F9DL);//using Lempel-Ziv-Welch algorithm
        allowedFileTypes.put("XZ", 0L);
        allowedFileTypes.put("lzma", 0L);
        allowedFileTypes.put("arj", 0L);
    }

    private boolean isFileAllowed(File file) {
        RandomAccessFile raf = null;
        try {
            //all zip files must start with the above signature
            //if they are valid zip files
            raf = new RandomAccessFile(file, "r");
            long firsBytes = raf.readInt();
            for (String fileType : allowedFileTypes.keySet()) {
                //review the known signatures
                for (long sign : allowedFileTypes.get(fileType)) {
                    //is this a known file?
                    if (sign == firsBytes) {
                        settings.setFileType(fileType);
                        return true;
                    }
                }
            }
            //if we reach this point we know that we can't determine
            //the file type with the file signature, let's use the manual
            //fashion
            String fileType = FilenameUtils.getExtension(file.getAbsolutePath());
            //is this file type supported?
            if (allowedFileTypes.containsKey(fileType)) {
                settings.setFileType(fileType);
                return true;
            } else {
                //we don't support this extension
                return false;
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e) {
                    //ignored
                }
            }
        }
    }

    public boolean validate() {
        File compressedFile;
        File directoryLocation;

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
            if (cmd.hasOption("i") && cmd.hasOption("o")) {
                //get argument values
                String sourceVal = cmd.getOptionValue("i");
                String targetVal = cmd.getOptionValue("o");
                //create file objects
                compressedFile = new File(sourceVal);
                directoryLocation = new File(targetVal);
                if (!compressedFile.canRead()) {
                    System.err.println("Can't read input file " + compressedFile.getPath());
                    return false;
                }

                if (!directoryLocation.isDirectory() && !directoryLocation.mkdirs()) {
                    System.err.println("Can't create output dir " + directoryLocation.getPath());
                    return false;
                }
                //review is this file type is allowed
                if (!isFileAllowed(compressedFile)) {
                    System.err.println("This file type is not allowed " + directoryLocation.getPath());
                    return false;
                }
                System.out.println("Decompress mode");

                settings.setCompress(false);
                settings.setDecompress(true);
                settings.setSource(compressedFile);
                settings.setTarget(directoryLocation);

                return true;
            }

            cmd = parser.parse(compressOpt, args, false);
            if (cmd.hasOption("c") && cmd.hasOption("t")) {
                //get argument values
                String targetVal = cmd.getOptionValue("c");
                String sourceVal = cmd.getOptionValue("t");
                //create file objects
                compressedFile = new File(sourceVal);
                directoryLocation = new File(targetVal);
                try {
                    if (!compressedFile.createNewFile()) {
                        System.err.println("Can't create compressed file" + compressedFile.getPath());
                        return false;
                    }
                } catch (IOException e) {
                    System.err.println("Can't create compressed file" + compressedFile.getPath());
                    return false;
                }

                if (!directoryLocation.canRead()) {
                    System.err.println("Can't read the target dir " + directoryLocation.getPath());
                    return false;
                }
                //review is this file type is allowed
                if (!isFileAllowed(compressedFile)) {
                    System.err.println("This file type is not allowed " + compressedFile.getPath());
                    return false;
                }
                System.out.println("Compress mode");

                settings.setCompress(true);
                settings.setDecompress(false);
                settings.setSource(compressedFile);
                settings.setTarget(directoryLocation);

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
