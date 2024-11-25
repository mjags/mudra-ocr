package com.mudra.ocr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class FileFilterService {
    public List<Path> getListOfUnProcessedFiles(String currentPath) throws IOException {
        List<Path> pathList = new ArrayList<>();
        Files.newDirectoryStream(Paths.get(""), path ->
                Files.isRegularFile(path) && path.toString().endsWith(".pdf")
        ).forEach(file -> {
            String filename = file.getFileName().toString();
            String outputFile = currentPath + "/" + (filename.substring(0, filename.lastIndexOf("."))) + ".docx";
            log.info(outputFile);
            if (!Files.exists(Paths.get(outputFile))) {
                pathList.add(file.toAbsolutePath());
            } else {
                log.error("****** | File already processed | ******");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.error(e.getMessage());
                }
            }
        });
        return pathList;
    }
}
