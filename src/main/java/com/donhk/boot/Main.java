package com.donhk.boot;

import com.donhk.Zip.Zip;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class Main {
    public static void main(String[] args) {
        try {
            //some basic validations
            if (args.length != 2) {
                System.out.println("Wrong number of arguments, try");
                System.out.println("java -jar zipper.jar /home/input.zip /home/output/dir");
            }

            File zipFile = new File(args[0]);
            File targetDir = new File(args[1]);

            if (!zipFile.canRead()) {
                System.out.println("Can't read input file " + zipFile.getPath());
            }

            if (!targetDir.isDirectory() && !targetDir.mkdirs()) {
                System.out.println("Can't create output dir " + targetDir.getPath());
            }

            //parameters looks valid
            long a = System.currentTimeMillis();
            Zip jZip = new Zip(zipFile.getCanonicalPath(), targetDir.getCanonicalPath());
            if (!jZip.prepare()) {
                System.out.println("There was a problem reading file contents");
            }
            cPrint(jZip.getTotalFiles() + " files will be unzipped");

            new Thread(() -> {
                try {
                    jZip.unzipFile();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            //show the number of files for a brief time
            Thread.sleep(3000);
            cPrint(String.join("", Collections.nCopies(30, " ")));
            //show progress
            while (jZip.getFilesRemaining() != 0) {
                cPrint(jZip.getFilesRemaining() + "/" + jZip.getTotalFiles());
                Thread.sleep(30);
            }
            cPrint("0/" + jZip.getTotalFiles());
            long b = System.currentTimeMillis();
            System.out.println();
            System.out.println("Done in " + ((b - a) / 1000) + " s");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void cPrint(String input) throws IOException {
        String line = "\r" + input;
        System.out.write(line.getBytes());
    }
}
