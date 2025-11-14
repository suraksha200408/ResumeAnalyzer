package com.resumeanalyzer;

public abstract class Document {
	private String filePath;

	public Document(String filePath) {
		this.filePath = filePath;
	}

	public String getFilePath() {
		return filePath;
	}

	public abstract String extractText();

}
