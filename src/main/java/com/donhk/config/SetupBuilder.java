package com.donhk.config;

import com.donhk.Zip.Zip;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class SetupBuilder {

    private final File source;
    private final File target;
    private final Settings settings;

    public SetupBuilder(Settings settings) {
        this.settings = settings;
        this.source = settings.getSource();
        this.target = settings.getTarget();
    }

    public void decompress() throws IOException, InterruptedException {
        //parameters looks valid
        switch (settings.getFileType()) {
            case "zip":
                break;
            case "7z":
                break;
            case "tar":
                break;
            case "tar.gz":
                break;
            case "bz2":
                break;
            case "z":
                break;
            case "tar.z":
                break;
            case "xz":
                break;
            case "lzma":
                break;
            case "arj":
                break;
        }
        long a = System.currentTimeMillis();
        Zip jZip = new Zip(source.getCanonicalPath(), target.getCanonicalPath());
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
            cPrint(String.join("", Collections.nCopies(30, " ")));
            cPrint(jZip.getFilesRemaining() + "/" + jZip.getTotalFiles());
            Thread.sleep(30);
        }
        cPrint("0/" + jZip.getTotalFiles());
        long b = System.currentTimeMillis();
        System.out.println();
        System.out.println("Done in " + ((b - a) / 1000) + " s");
    }

    public void compress() {
        long a = System.currentTimeMillis();
        long b = System.currentTimeMillis();
        System.out.println();
        System.out.println("Done in " + ((b - a) / 1000) + " s");
    }


    private static void cPrint(String input) throws IOException {
        String line = "\r" + input;
        System.out.write(line.getBytes());
    }

}
