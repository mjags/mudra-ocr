package com.mudra.ocr.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;

@Service
@Slf4j
public class FileGeneratorService {

    private FileOutputStream OUT_STREAM = null;

    private static XWPFDocument DOCUMENT = new XWPFDocument();

    public void createDocument(String outputFileName) {
        try {
            OUT_STREAM = new FileOutputStream(outputFileName);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void closeDocument() {
        try {
            OUT_STREAM.close();
            DOCUMENT.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public XWPFRun newParagraph() {
        XWPFParagraph paragraph = DOCUMENT.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = paragraph.createRun();
        titleRun.setFontFamily("Gautami");
        titleRun.setColor("000000");
        titleRun.setFontSize(14);
        return titleRun;
    }

    public void addText(XWPFRun paragraph, String text){
        paragraph.setText(text);
    }

    public void addLineBreak(XWPFRun xwpfRun){
        xwpfRun.addBreak();
    }

    public void addSpace(XWPFRun xwpfRun){
        xwpfRun.setText(" ");
    }

    public void writeToDocument() {
        try {
            DOCUMENT.write(OUT_STREAM);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    public void generateWordFile(String text, String outputFileName) {
        XWPFDocument document = new XWPFDocument();
        FileOutputStream out = null;
        try {
            //--create word paragraph
            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.LEFT);
            XWPFRun titleRun = title.createRun();
            titleRun.setFontFamily("Gautami");
            titleRun.setText(text);
            titleRun.setColor("000000");
            titleRun.setFontSize(16);
            //--create file
            out = new FileOutputStream(outputFileName);
            document.write(out);
            out.close();
            document.close();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


}
