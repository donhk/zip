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

public class Zip {

    private ZipFile myZip;
    private int BUFFER = 1024 * 128;
    private CountDownLatch latch = new CountDownLatch(0);
    private final File zipFile;
    private final File targetPath;
    private int totalFiles = 0; //total files that will be processed
    private boolean IS_WINDOWS = System.getProperty("os.name").contains("windows");
    
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

    /**
     * Decompress using the Java's Zip API, this method doesn't work properly on UNIX like OS
     * due to the lack of support to read the permissions of the files and thus they can't be
     * restored to the values they had prior the compression and that is way this is only used
     * as a primary option for Windows
     *
     * @return true if the unzip was successfully, false otherwise
     * @throws IOException if there was a problem creating the process that calls the unzip or IO error
     */
    private boolean decompressFile() throws IOException, InterruptedException {
        // create output directory if it doesn't exist
        if (!targetPath.exists()) {
            if (!targetPath.mkdirs()) {
                //error creating directory
                return false;
            }
        }
        List<ZipJob> zipJobs = new ArrayList<>();
        int cores = 4;
        ExecutorService executor = Executors.newFixedThreadPool(cores);
        System.out.println("Analyzing zip file " + zipFile.getAbsolutePath());
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
        System.out.println("start unzipping: " + zipFile.getAbsolutePath() + " on " + targetPath);

        executor.invokeAll(zipJobs);
        latch.await();
        executor.shutdown();

        return true;
    }

    /**
     * Unzips the file
     *
     * @return true if the file extraction was successfully, false otherwise
     * @throws IOException          if there was a problem creating the process that calls the unzip or IO error
     * @throws InterruptedException if the operation was interrupted finishes
     */
    public boolean unzipFile() throws IOException, InterruptedException {
        //validate zip file
        if (!validateZipFile()) {
            return false;
        }
        //decompress file
        return decompressFile();
    }

    /**
     * number of files remaining that will be decompressed, this only works on windows
     *
     * @return number of files remaining to be processed
     */
    public int getFilesRemaining() {
        return (int) latch.getCount();
    }

    public int getTotalFiles() {
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

        public String call() throws Exception {
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
            latch.countDown();
            return destFile.getAbsolutePath();
        }
    }

}
