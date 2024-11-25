package com.mudra.ocr.service;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.AnnotateFileResponse;
import com.google.cloud.vision.v1.AsyncAnnotateFileRequest;
import com.google.cloud.vision.v1.AsyncAnnotateFileResponse;
import com.google.cloud.vision.v1.AsyncBatchAnnotateFilesResponse;
import com.google.cloud.vision.v1.Block;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.GcsDestination;
import com.google.cloud.vision.v1.GcsSource;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.InputConfig;
import com.google.cloud.vision.v1.OperationMetadata;
import com.google.cloud.vision.v1.OutputConfig;
import com.google.cloud.vision.v1.Paragraph;
import com.google.cloud.vision.v1.Symbol;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.Word;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service("asyncTeluguOcrService")
@Qualifier("asyncTeluguOcrService")
public class AsyncTeluguOcrService implements OcrService {

    @Autowired
    private FileGeneratorService fileGeneratorService;

    @Override
    public void processFile(String gcsSourcePath) throws IOException {
        String gcsDestinationPath = "gs://mudra-ocr-telugu/dest/";
        // Initialize client that will be used to send requests. This client only needs to be created
        // once, and can be reused for multiple requests. After completing all of your requests, call
        // the "close" method on the client to safely clean up any remaining background resources.
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            ArrayList<AsyncAnnotateFileRequest> requests;
            requests = new ArrayList<>();

            // Set the GCS source path for the remote file.
            GcsSource gcsSource = GcsSource.newBuilder().setUri(gcsSourcePath).build();

            // Create the configuration with the specified MIME (Multipurpose Internet Mail Extensions)
            // types
            InputConfig inputConfig =
                    InputConfig.newBuilder()
                            .setMimeType(
                                    "application/pdf") // Supported MimeTypes: "application/pdf", "image/tiff"
                            .setGcsSource(gcsSource)
                            .build();

            // Set the GCS destination path for where to save the results.
            GcsDestination gcsDestination =
                    GcsDestination.newBuilder().setUri(gcsDestinationPath).build();

            // Create the configuration for the System.output with the batch size.
            // The batch size sets how many pages should be grouped into each json System.output file.
            OutputConfig outputConfig =
                    OutputConfig.newBuilder().setBatchSize(4).setGcsDestination(gcsDestination).build();

            // Select the Feature required by the vision API
            Feature feature = Feature.newBuilder().setType(Feature.Type.DOCUMENT_TEXT_DETECTION).build();

            // Build the OCR request
            AsyncAnnotateFileRequest request =
                    AsyncAnnotateFileRequest.newBuilder()
                            .addFeatures(feature)
                            .setInputConfig(inputConfig)
                            .setOutputConfig(outputConfig)
                            .build();

            requests.add(request);

            // Perform the OCR request
            OperationFuture<AsyncBatchAnnotateFilesResponse, OperationMetadata> response =
                    client.asyncBatchAnnotateFilesAsync(requests);

            log.info("Waiting for the operation to finish.");

            // Wait for the request to finish.
            // (The result is not used, since the API saves the result to the specified location on GCS.)
            List<AsyncAnnotateFileResponse> result =
                    response.get(180, TimeUnit.SECONDS).getResponsesList();

        } catch (InterruptedException e) {
            log.error(e.getMessage());
        } catch (ExecutionException e) {
            log.error(e.getMessage());
        } catch (TimeoutException e) {
            log.error(e.getMessage());
        }
    }

    private List<String> listFiles(String dir) {
        return Stream.of(new File(dir).listFiles())
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toList());
    }

    private void processImages() {

        List<String> sortedList = listFiles("./images");
        Collections.sort(sortedList);
        System.out.println(sortedList);

        Document doc = null;
        try {
            Image firstImage = new Image(ImageDataFactory.create("./images/" + sortedList.get(0)));
            PdfWriter writer = new PdfWriter("source.pdf");
            PdfDocument pdfDoc = new PdfDocument(writer);
            // Creating a Document
            doc = new Document(pdfDoc, new PageSize(firstImage.getImageWidth(), firstImage.getImageHeight()));
            Document finalDoc = doc;
            final int[] index = {1};
            sortedList.stream().sorted().forEach(fileName -> {
                String filename = "./images/" + fileName;
                ImageData data = null;
                try {
                    data = ImageDataFactory.create(filename);
                } catch (MalformedURLException e) {
                    log.error(e.getMessage());
                }
                Image image = new Image(data);
                pdfDoc.addNewPage(new PageSize(image.getImageWidth(), image.getImageHeight()));
                image.setFixedPosition(index[0], 0, 0);
                finalDoc.add(image);
                index[0]++;
            });
            pdfDoc.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        } finally {
            if (doc != null) {
                doc.close();
            }
        }

    }

    private String uploadPdfToGcs() {

        String BUCKET_NAME = "mudra-ocr-telugu";
        String BLOB_NAME = "src/source.pdf";

        Storage storage = StorageOptions.getDefaultInstance().getService();
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(new File("source.pdf"));
            Bucket bucket = storage.get(BUCKET_NAME);
            Blob blob = bucket.create(BLOB_NAME, inputStream, "application/pdf");
            log.info("Generated Id: {}", blob.getGeneratedId());
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        }
        return "gs://" + BUCKET_NAME + "/" + BLOB_NAME;
    }

    @Override
    public void processFileFromGCS(String gcsSourcePath) throws IOException {
        //processImages();
        String path = uploadPdfToGcs();
        if (StringUtils.isNotEmpty(path)) {
            log.info("File Uploaded: {}", path);
            processFile(path);
            processDestinationBlob();
        }
    }

    private void processDestinationBlob() {
        try {
            // Once the request has completed and the System.output has been
            // written to GCS, we can list all the System.output files.
            Storage storage = StorageOptions.getDefaultInstance().getService();

            // Get the list of objects with the given prefix from the GCS bucket
            Bucket bucket = storage.get("mudra-ocr-telugu");
            Page<Blob> blobs =
                    bucket.list(
                            Storage.BlobListOption.prefix("dest/"),
                            Storage.BlobListOption.currentDirectory());

            Map<Integer, Blob> jsonFileMap = new HashMap<>();

            // List objects with the given prefix.
            log.info("Output files:");
            for (Blob blob : blobs.iterateAll()) {
                String blobName = blob.getName();
                log.info(blobName);


                // Process the first System.output file from GCS.
                // Since we specified batch size = 2, the first response contains
                // the first two pages of the input file.
                if (blob.getName().endsWith("/")) {
                    continue;
                }

                int index = blobName.indexOf('-');
                Integer indexKey = Integer.parseInt(blobName.substring(index + 1, blobName.indexOf("-", index + 1)));
                log.info("Index: {}", indexKey);

                jsonFileMap.put(indexKey, blob);
            }

            LinkedHashMap<Integer, Blob> sortedMap = new LinkedHashMap<>();
            jsonFileMap.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));

            fileGeneratorService.createDocument("finalDraft.docx");

            sortedMap.entrySet().stream().forEach(ele -> {
                String jsonContents = new String(ele.getValue().getContent());
                AnnotateFileResponse.Builder builder = AnnotateFileResponse.newBuilder();
                try {
                    JsonFormat.parser().merge(jsonContents, builder);
                } catch (InvalidProtocolBufferException e) {
                    log.error(e.getMessage());
                }
                // Build the AnnotateFileResponse object
                AnnotateFileResponse annotateFileResponse = builder.build();

                // Parse through the object to get the actual response for the first page of the input file.
                annotateFileResponse.getResponsesList().stream().forEach(annotateImageResponse -> {
                    generateFile(annotateImageResponse.getFullTextAnnotation());
                    log.info("{}", annotateImageResponse.getFullTextAnnotation().getText());
                });
            });
            fileGeneratorService.writeToDocument();
            fileGeneratorService.closeDocument();

        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void generateFile(TextAnnotation textAnnotation) {

        for (com.google.cloud.vision.v1.Page page : textAnnotation.getPagesList()) {
            for (Block block : page.getBlocksList()) {
                XWPFRun paraGraph = fileGeneratorService.newParagraph();
                for (Paragraph par : block.getParagraphsList()) {
                    for (Word word : par.getWordsList()) {
                        for (Symbol symbol : word.getSymbolsList()) {
                            if (StringUtils.isNotEmpty(symbol.getText())) {
                                fileGeneratorService.addText(paraGraph, symbol.getText());
                            }
                            if (symbol.getProperty().getDetectedBreak().isInitialized()) {
                                if (symbol.getProperty().getDetectedBreak().getType().equals(TextAnnotation.DetectedBreak.BreakType.SPACE)) {
                                    fileGeneratorService.addSpace(paraGraph);
                                }
                                if (symbol.getProperty().getDetectedBreak().getType().equals(TextAnnotation.DetectedBreak.BreakType.EOL_SURE_SPACE)) {
                                    fileGeneratorService.addSpace(paraGraph);
                                    fileGeneratorService.addLineBreak(paraGraph);
                                }
                                if (symbol.getProperty().getDetectedBreak().getType().equals(TextAnnotation.DetectedBreak.BreakType.LINE_BREAK)) {
                                    fileGeneratorService.addLineBreak(paraGraph);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
