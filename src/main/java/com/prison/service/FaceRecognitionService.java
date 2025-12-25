package com.prison.service;

import com.prison.util.PythonRunnerUtil;

public class FaceRecognitionService {

    public String recognize() {
        return PythonRunnerUtil.runRecognition();
    }
}
