/* Copyright (c) 2017 Frederick Alvarez
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

/*
   DESCRIPTION
    Class in charge of decompress a zip file on a target directory

   PRIVATE CLASSES
    N/A

   NOTES
    N/A

   MODIFIED  (MM/DD/YY)
    donhk     11/25/17 - Creation
 */
package com.donhk.Zip;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author donhk
 */
public class Zip {

    private ZipFile myZip;
    private int BUFFER = 1024 * 128;
    private CountDownLatch latch = new CountDownLatch(0);
    private final File zipFile;
    private final File targetPath;
    private long totalFiles = 0; //total files that will be processed
    private List<ZipJob> zipJobs = new ArrayList<>();
    private boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");

    //Unix file flags
    private final int S_IRUSR = 256;   // OWNER_READ
    private final int S_IWUSR = 128;   // OWNER_WRITE
    private final int S_IXUSR = 64;    // OWNER_EXECUTE
    private final int S_IRGRP = 32;    // GROUP_READ
    private final int S_IWGRP = 16;    // GROUP_WRITE
    private final int S_IXGRP = 8;     // GROUP_EXECUTE
    private final int S_IROTH = 4;     // OTHERS_READ
    private final int S_IWOTH = 2;     // OTHERS_WRITE
    private final int S_IXOTH = 1;     // OTHERS_EXECUTE

    public Zip(String zipFile, String targetPath) {
        this.zipFile = new File(zipFile);
        this.targetPath = new File(targetPath);
    }

    public boolean prepare() throws IOException {
        if (!validateZipFile()) {
            return false;
        }
        // create output directory if it doesn't exist
        if (!targetPath.exists()) {
            if (!targetPath.mkdirs()) {
                //error creating directory
                return false;
            }
        }

        System.out.println("Reading zip file " + zipFile.getAbsolutePath());
        myZip = new ZipFile(zipFile);
        Enumeration<? extends ZipArchiveEntry> zipFileEntries = myZip.getEntries();
        int filesRemaining = 0;
        //perform a pre-count so that we can know how many files
        //will be unzipped and thus do some maths to track the progress
        while (zipFileEntries.hasMoreElements()) {
            ZipArchiveEntry entry = zipFileEntries.nextElement();
            if (!entry.isDirectory()) {
                zipJobs.add(new ZipJob(entry));
                filesRemaining++;
            }
        }
        totalFiles = filesRemaining;
        latch = new CountDownLatch(filesRemaining);
        return true;
    }

    /**
     * Decompress using the Apaches's Zip API + homemade code which gives support for permission
     * restoration, symlinks handling and parallelism
     *
     * @return true if the file extraction was successfully, false otherwise
     * @throws IOException          if there was a problem creating the process that calls the unzip or IO error
     * @throws InterruptedException if the operation was interrupted finishes
     */
    public boolean unzipFile() throws IOException, InterruptedException {
        int cores = Runtime.getRuntime().availableProcessors();
        if (cores > 8) {
            cores = 8;
        }
        ExecutorService executor = Executors.newFixedThreadPool(cores);
        executor.invokeAll(zipJobs);
        latch.await();
        executor.shutdown();
        return true;
    }

    /**
     * @return number of files remaining of being decompressed
     */
    public long getFilesRemaining() {
        return latch.getCount();
    }

    /**
     * @return total files which will be unzipped
     */
    public long getTotalFiles() {
        return totalFiles;
    }

    /**
     * Sets the unix permissions over a file
     *
     * @param filePermissions decimal representation of permission of the file
     * @param targetFile      file to which the file permission will be applied
     * @throws IOException if there was an error setting the permissions
     */
    private void restorePermissions(int filePermissions, String targetFile) throws IOException {
        //clean up extra bytes
        int permissions = filePermissions & 0xFFF;

        //using PosixFilePermission to set file permissions 777
        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        //add owner permission
        if ((permissions & S_IRUSR) != 0) {
            perms.add(PosixFilePermission.OWNER_READ);
        }
        if ((permissions & S_IWUSR) != 0) {
            perms.add(PosixFilePermission.OWNER_WRITE);
        }
        if ((permissions & S_IXUSR) != 0) {
            perms.add(PosixFilePermission.OWNER_EXECUTE);
        }
        //add group permissions
        if ((permissions & S_IRGRP) != 0) {
            perms.add(PosixFilePermission.GROUP_READ);
        }
        if ((permissions & S_IWGRP) != 0) {
            perms.add(PosixFilePermission.GROUP_WRITE);
        }
        if ((permissions & S_IXGRP) != 0) {
            perms.add(PosixFilePermission.GROUP_EXECUTE);
        }
        //add others permissions
        if ((permissions & S_IROTH) != 0) {
            perms.add(PosixFilePermission.OTHERS_READ);
        }
        if ((permissions & S_IWOTH) != 0) {
            perms.add(PosixFilePermission.OTHERS_WRITE);
        }
        if ((permissions & S_IXOTH) != 0) {
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        Files.setPosixFilePermissions(Paths.get(targetFile), perms);
    }

    /**
     * Verifies that the zip file is real zip file and not another type of file
     *
     * @return true of the file is a zup file, false otherwise
     */
    private boolean validateZipFile() {
        //https://en.wikipedia.org/wiki/List_of_file_signatures
        long ZIP_SIGNATURE = 0x504B0304;
        long ZIP_EMPTY_FILE = 0x504B0506;
        long ZIP_SPANNED_FILE = 0x504B0708;
        RandomAccessFile raf = null;
        try {
            //all zip files must start with the above signature
            //if they are valid zip files

            raf = new RandomAccessFile(zipFile, "r");
            long firsBytes = raf.readInt();

            if (firsBytes == ZIP_EMPTY_FILE) {
                //the zip file is empty
                return false;
            } else if (firsBytes == ZIP_SPANNED_FILE) {
                //not supported format
                return false;
            } else if (firsBytes == ZIP_SIGNATURE) {
                //the file is valid
                return true;
            } else {
                //this is not a zip file
                return false;
            }
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException ioex) {
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

    private class ZipJob implements Callable<String> {
        private ZipArchiveEntry entry;

        ZipJob(ZipArchiveEntry entry) {
            this.entry = entry;
        }

        public String call() {
            try {
                String fileName = entry.getName();
                File destFile = new File(targetPath, fileName);
                File destinationParent = destFile.getParentFile();
                //recreate original structure
                destinationParent.mkdirs();

                if (entry.isUnixSymlink() && !IS_WINDOWS) {
                    Files.createSymbolicLink(destFile.toPath(), Paths.get(myZip.getUnixSymlink(entry)));
                } else {
                    BufferedInputStream is = new BufferedInputStream(myZip.getInputStream(entry));
                    int currentByte;
                    // establish buffer for writing file
                    byte data[] = new byte[BUFFER];

                    // write the current file to disk
                    FileOutputStream fos = new FileOutputStream(destFile);
                    BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);

                    // read and write until last byte is encountered
                    while ((currentByte = is.read(data, 0, BUFFER)) != -1) {
                        dest.write(data, 0, currentByte);
                    }
                    dest.flush();
                    dest.close();
                    is.close();

                    //restore permissions if this is not windows
                    if (!IS_WINDOWS) {
                        restorePermissions(entry.getUnixMode(), destFile.getAbsolutePath());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
            return "done";
        }
    }

}
