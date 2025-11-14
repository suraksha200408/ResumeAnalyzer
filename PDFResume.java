package com.resumeanalyzer;

import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PDFResume {

	public static String extractText(String filePath) throws IOException {
		try (PDDocument document = PDDocument.load(new File(filePath))) {
			PDFTextStripper stripper = new PDFTextStripper();
			return stripper.getText(document);
		}
	}
}

