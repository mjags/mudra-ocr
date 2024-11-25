package com.mudra.ocr.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@Service
@Slf4j
public class ImageExtractorService extends PDFStreamEngine {
    public int imageNumber = 1;

    public void extractAndStoreImages(String filePath) {
        PDDocument document = null;
        try {
            document = PDDocument.load(Paths.get(filePath).toFile());
            log.info(document.getPages().toString());
            processPage(document.getPage(0));

//            PDFRenderer renderer = new PDFRenderer(document);
//            BufferedImage image = renderer.renderImage(0);
//            ImageIO.write(image, "JPEG", new File(Paths.get("").toAbsolutePath().toString() + "/test1.jpg"));
//            log.info("Images are extracted");
            document.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    protected void processOperator(Operator operator, List<COSBase> operands) throws IOException {
        String operation = operator.getName();
        if ("Do".equals(operation)) {
            COSName objectName = (COSName) operands.get(0);
            PDXObject xobject = getResources().getXObject(objectName);
            if (xobject instanceof PDImageXObject) {
                PDImageXObject image = (PDImageXObject) xobject;
                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();

                // same image to local
                BufferedImage bImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
                bImage = image.getImage();
                ImageIO.write(bImage, "PNG", new File("image_" + imageNumber + ".png"));
                System.out.println("Image saved.");
                imageNumber++;

            } else if (xobject instanceof PDFormXObject) {
                PDFormXObject form = (PDFormXObject) xobject;
                showForm(form);
            }
        } else {
            super.processOperator(operator, operands);
        }
    }
}
