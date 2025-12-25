package com.prison.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class PythonRunnerUtil {

    private static final String PYTHON = "python";
    private static final String PYTHON_DIR = "python-face";

    public static String trainFace(String personType, int personId) {

        return runProcess(new String[]{
                PYTHON,
                "train_faces.py",
                personType,
                String.valueOf(personId)
        });
    }

    public static String runRecognition() {

        return runProcess(new String[]{
                PYTHON,
                "recognize_face.py"
        });
    }

    private static String runProcess(String[] command) {

        String lastLine = null;

        try {
            ProcessBuilder pb = new ProcessBuilder(command);

            // 🔥 THIS IS THE KEY FIX
            pb.directory(new File(PYTHON_DIR));

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line); // keep debug
                lastLine = line;
            }

            return lastLine;

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR|JAVA|0";
        }
    }
}
