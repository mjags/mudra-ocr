package com.mudra.ocr.service;

import java.io.IOException;

public interface OcrService {
    void processFile(String directory) throws IOException;
    void processFileFromGCS(String gcsSourcePath) throws IOException;
}
