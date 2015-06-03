package com.mnemonic.importer;


import com.mnemonic.db.DbHelper;
import com.mnemonic.db.Test;
import com.mnemonic.db.TestGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;


public class Importer {

    private static final String TEST_MARKER = "!";

    private static final String COMMENT_MARKER = "#";

    private static final Pattern SPLITTER_PATTERN = Pattern.compile("\\s*=\\s*");

    private final DbHelper dbHelper;

    public Importer(DbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public TestGroup importTestGroup(final String groupName, final InputStream inputStream) throws ImportException {
        try {
            return dbHelper.runTransactional(new Callable<TestGroup>() {

                @Override
                public TestGroup call() throws IOException {
                    TestGroup testGroup = dbHelper.addTestGroup(groupName);

                    importTests(testGroup, new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)));

                    return testGroup;
                }
            });
        } catch (Exception e) {
            throw new ImportException(e);
        }
    }

    private void importTests(TestGroup testGroup, BufferedReader bufferedReader) throws IOException {
        Test currentTest = null;

        boolean taskStarted = false;
        String question = null;
        String answer = null;

        String originalLine;
        String trimmedLine;
        while ((originalLine = bufferedReader.readLine()) != null) {
            trimmedLine = originalLine.trim();
            if (trimmedLine.startsWith(COMMENT_MARKER)) {
                continue;
            }

            if (trimmedLine.isEmpty()) {
                if (taskStarted) {
                    dbHelper.addTask(currentTest, question, answer);
                    taskStarted = false;
                }
                currentTest = null;

                continue;
            }

            if (trimmedLine.startsWith(TEST_MARKER)) {
                // test header encountered
                // finish previous task, if any
                if (taskStarted) {
                    dbHelper.addTask(currentTest, question, answer);
                }

                // test header encountered, try to extract the name and description
                String name;
                String description;

                String header = trimmedLine.substring(1).trim();
                String[] tokens = SPLITTER_PATTERN.split(header, 2);
                switch (tokens.length) {
                    case 1:
                        name = tokens[0].trim();
                        description = null;
                        break;

                    default:
                        name = tokens[0].trim();
                        description = tokens[1].trim();
                        break;
                }

                if (name.isEmpty()) {
                    name = null;
                }
                if (description != null && description.isEmpty()) {
                    description = null;
                }
                currentTest = dbHelper.addTest(testGroup, name, description);
                taskStarted = false;
            } else {
                // task started
                if (currentTest == null) {
                    // first task in a test without header
                    currentTest = dbHelper.addTest(testGroup, null, null);
                    taskStarted = false;
                }

                if (Character.isWhitespace(originalLine.codePointAt(0)) && taskStarted) {
                    // continuation of the previous task's answer
                    answer += "\n" + trimmedLine;
                } else {
                    // first line in test (leniently allow leading whitespace) or any other task line
                    // finish previous task, if any
                    if (taskStarted) {
                        dbHelper.addTask(currentTest, question, answer);
                    }

                    taskStarted = true;
                    String[] tokens = SPLITTER_PATTERN.split(trimmedLine, 2);
                    switch (tokens.length) {
                        case 1:
                            question = tokens[0].trim();
                            answer = null;
                            break;

                        default:
                            question = tokens[0].trim();
                            answer = tokens[1].trim();
                            break;
                    }
                }
            }
        }
        // finish the last task, if any
        if (taskStarted) {
            dbHelper.addTask(currentTest, question, answer);
        }
    }
}
