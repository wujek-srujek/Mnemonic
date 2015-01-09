package com.mnemonic;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;


public class TestParser {

    public List<Test> parse(File file, String defaultTestNameFormat) {
        List<Test> tests = new LinkedList<Test>();

        LineNumberReader reader = null;
        try {
            reader = new LineNumberReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));

            boolean testStarted = false;
            String name = null;
            String description = null;
            int firstLineNumber = 0;
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    line = line.trim();
                }

                if (line != null && !line.isEmpty() && !testStarted) {
                    testStarted = true;
                    firstLineNumber = reader.getLineNumber();
                    if (line.startsWith("#")) {
                        ++firstLineNumber;
                        String header = line.substring(1).trim();
                        String[] tokens = header.split("\\w*=\\w*", 2);
                        switch (tokens.length) {
                            case 1:
                                name = tokens[0].trim();
                                break;

                            case 2:
                                name = tokens[0].trim();
                                description = tokens[1].trim();
                                break;
                        }
                    }

                    if (name == null || name.isEmpty()) {
                        // test without header or name
                        name = String.format(defaultTestNameFormat, tests.size() + 1);
                    }
                    if (description != null && description.isEmpty()) {
                        description = null;
                    }
                }

                if (line == null || line.isEmpty()) {
                    if (testStarted) {
                        int lastLineNumber = reader.getLineNumber();
                        if (line != null && lastLineNumber != firstLineNumber) {
                            --lastLineNumber;
                        }
                        tests.add(new Test(name, description, file, firstLineNumber, lastLineNumber));
                        testStarted = false;
                        name = null;
                        description = null;
                        firstLineNumber = 0;
                    }
                    if (line == null) {
                        break;
                    }
                }
            }
        } catch (IOException exc) {
            // nothing, just return as much as there is
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException exc) {
                // ignore, just don't propagate up
            }
        }

        return tests;
    }
}