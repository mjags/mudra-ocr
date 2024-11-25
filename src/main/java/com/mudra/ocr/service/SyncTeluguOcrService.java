package com.mudra.ocr.service;

import com.google.cloud.vision.v1.AnnotateFileRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateFilesRequest;
import com.google.cloud.vision.v1.BatchAnnotateFilesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.InputConfig;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gcp.vision.CloudVisionTemplate;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service(value = "syncTeluguOcrService")
@Slf4j
@Qualifier("syncTeluguOcrService")
public class SyncTeluguOcrService { //implements OcrService {

    @Autowired
    private FileFilterService fileFilterService;

    @Autowired
    private CloudVisionTemplate cloudVisionTemplate;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private ImageExtractorService imageExtractorService;

    @Autowired
    private FileGeneratorService fileGeneratorService;

    public String batchAnnotateFiles(String filePath, int pageCount) throws IOException {
        try (ImageAnnotatorClient imageAnnotatorClient = ImageAnnotatorClient.create()) {
            Path path = Paths.get(filePath);
            byte[] data = Files.readAllBytes(path);
            ByteString content = ByteString.copyFrom(data);

            InputConfig inputConfig =
                    InputConfig.newBuilder().setMimeType("application/pdf").setContent(content).build();
            Feature feature = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();

            int pageIndex = 1;
            StringBuilder fullText = new StringBuilder();
            while (pageIndex <= pageCount) {
                AnnotateFileRequest fileRequest =
                        AnnotateFileRequest.newBuilder()
                                .setInputConfig(inputConfig)
                                .addFeatures(feature)
                                .addPages(pageIndex)
                                .build();

                BatchAnnotateFilesRequest request =
                        BatchAnnotateFilesRequest.newBuilder().addRequests(fileRequest).build();
                BatchAnnotateFilesResponse response = imageAnnotatorClient.batchAnnotateFiles(request);

                for (AnnotateImageResponse imageResponse :
                        response.getResponsesList().get(0).getResponsesList()) {
                    fullText.append(imageResponse.getFullTextAnnotation().getText());
                }
                pageIndex++;
            }
            return fullText.toString();
        }
    }

   // @Override
    public void processFile(String directory) throws IOException {
        String currentPath = Paths.get("").toAbsolutePath().toString();
        List<Path> pathList = fileFilterService.getListOfUnProcessedFiles(currentPath);
        pathList.stream().forEach(path -> {
            String fileName = path.getFileName().toString();
            log.info(fileName);
            try {
                PDDocument document = PDDocument.load(Paths.get(currentPath + "/" + fileName).toFile());
                log.info("---Pages--- {}", document.getPages().getCount());
                document.close();
                String text = batchAnnotateFiles(currentPath + "/" + fileName, document.getPages().getCount());
                //imageExtractorService.extractAndStoreImages(currentPath + "/" + fileName);
                fileGeneratorService.generateWordFile(text, fileName.substring(0, fileName.indexOf(".")) + ".docx");
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });

    }

   // @Override
    public void processFileFromGCS(String gcsSourcePath) throws IOException {

    }
}

//                for (Page page : imageResponse.getFullTextAnnotation().getPagesList()) {
//                    for (Block block : page.getBlocksList()) {
//                        for (Paragraph par : block.getParagraphsList()) {
//                            StringBuilder builder = new StringBuilder();
//                            for (Word word : par.getWordsList()) {
//                                for (Symbol symbol : word.getSymbolsList()) {
//                                    if (word.getProperty().getDetectedBreak().getType() == TextAnnotation.DetectedBreak.BreakType.SPACE) {
//                                        builder.append(" ");
//                                    }
//                                    if (StringUtils.isNotEmpty(symbol.getText())) {
//                                        builder.append(symbol.getText());
//                                    }
//                                }
//                            }
//                            //System.out.println(builder.toString());
//                        }
//                    }
//                }
