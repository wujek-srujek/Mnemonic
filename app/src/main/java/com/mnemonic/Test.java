package com.mnemonic;


import java.io.File;


public class Test {

    private final String name;

    private final String description;

    private final File file;

    private final int firstLineNumber;

    private final int lastLineNumber;

    public Test(String name, String description, File file, int firstLineNumber, int lastLineNumber) {
        this.name = name;
        this.description = description;
        this.file = file;
        this.firstLineNumber = firstLineNumber;
        this.lastLineNumber = lastLineNumber;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public File getFile() {
        return file;
    }

    public int getFirstLineNumber() {
        return firstLineNumber;
    }

    public int getLastLineNumber() {
        return lastLineNumber;
    }

    @Override
    public String toString() {
        return String.format("Test: name=%s, description=%s, file=%s, firstLineNumber=%d, lastLineNumber=%d ",
                name, description, file, firstLineNumber, lastLineNumber);
    }
}