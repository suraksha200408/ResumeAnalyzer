package com.resumeanalyzer;

import java.io.File;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class TestPDF {

	// Method that extracts text and returns it
	public static String extractText(String filePath) {
		String text = "";
		try (PDDocument document = PDDocument.load(new File(filePath))) {
			PDFTextStripper stripper = new PDFTextStripper();
			text = stripper.getText(document);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return text;
	}

}
