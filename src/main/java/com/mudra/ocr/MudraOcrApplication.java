package com.mudra.ocr;

import com.mudra.ocr.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;

@SpringBootApplication
@Slf4j
@ConfigurationProperties("spring.cloud.gcp.vision")
public class MudraOcrApplication implements CommandLineRunner {

    @Autowired
    private OcrService asyncTeluguOcrService;

    public static void main(String[] args) {
        SpringApplication.run(MudraOcrApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Mudra OCR Application Started !!!");
        String FOLDER_TO_SCAN = "";
        if (args.length > 0 && args[0] != null) {
            FOLDER_TO_SCAN = args[0];
        }
        asyncTeluguOcrService.processFileFromGCS(FOLDER_TO_SCAN);
    }

}
