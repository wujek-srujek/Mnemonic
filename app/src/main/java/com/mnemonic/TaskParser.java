package com.mnemonic;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TaskParser {

    public List<Task> parse(File file, int firstLineNumber, int lastLineNumber) {
        if (firstLineNumber == lastLineNumber) {
            return Collections.emptyList();
        }

        List<Task> tasks = new ArrayList<Task>(lastLineNumber - firstLineNumber + 1);

        boolean taskStarted = false;
        String question = null;
        String answer = null;
        LineNumberReader reader = null;
        try {
            reader = new LineNumberReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            while (reader.getLineNumber() < firstLineNumber - 1) {
                reader.readLine();
            }
            while (true) {
                String line = reader.readLine();
                if (line.startsWith("#")) {
                    continue;
                }

                if (Character.isWhitespace(line.codePointAt(0)) && taskStarted) {
                    // continuation of the previous task's answer
                    answer += "\n" + line.trim();
                } else {
                    // first line in test (we leniently allow leading whitespace) or standard task line
                    // finish previous task, if any
                    if (taskStarted) {
                        tasks.add(new Task(question, answer));
                        question = null;
                        answer = null;
                    }

                    taskStarted = true;
                    String[] tokens = CommonConstants.SPLITTER_PATTERN.split(line, 2);
                    switch (tokens.length) {
                        case 1:
                            question = tokens[0].trim();
                            break;

                        case 2:
                            question = tokens[0].trim();
                            answer = tokens[1].trim();
                            break;
                    }

                    if (reader.getLineNumber() == lastLineNumber) {
                        tasks.add(new Task(question, answer));
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

        return tasks;
    }
}