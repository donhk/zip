package com.donhk.config;

import java.io.File;

public class Settings {

    private File source;
    private File target;
    private boolean decompress = false;
    private boolean compress = false;
    private String fileType = null;

    Settings() {
    }

    Settings(File source, File target, boolean decompress, boolean compress) {
        this.source = source;
        this.target = target;
        this.decompress = decompress;
        this.compress = compress;
    }

    public File getSource() {
        return source;
    }

    public File getTarget() {
        return target;
    }

    public boolean isDecompress() {
        return decompress;
    }

    public boolean isCompress() {
        return compress;
    }

    public String getFileType() {
        return fileType;
    }

    public void setSource(File source) {
        this.source = source;
    }

    public void setTarget(File target) {
        this.target = target;
    }

    public void setDecompress(boolean decompress) {
        this.decompress = decompress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}
