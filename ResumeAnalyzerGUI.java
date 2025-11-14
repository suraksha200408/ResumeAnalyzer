package com.resumeanalyzer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

public class ResumeAnalyzerGUI extends JFrame {

	private JTextPane resultPane;
	private JTextArea jobDescArea;
	private File selectedPDF;

	public ResumeAnalyzerGUI() {
		setTitle("AI-Powered Resume Analyzer");
		setSize(700, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLayout(new BorderLayout(10, 10));

		// ===== HEADER =====
		JLabel titleLabel = new JLabel("AI-Powered Resume Analyzer", JLabel.CENTER);
		titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
		add(titleLabel, BorderLayout.NORTH);

		// ===== CENTER PANEL =====
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new GridLayout(2, 1, 10, 10));

		// Job Description Panel
		JPanel jobPanel = new JPanel(new BorderLayout());
		jobPanel.setBorder(BorderFactory.createTitledBorder("Job Description"));
		jobDescArea = new JTextArea(5, 40);
		jobDescArea.setLineWrap(true);
		jobDescArea.setWrapStyleWord(true);
		jobPanel.add(new JScrollPane(jobDescArea), BorderLayout.CENTER);
		centerPanel.add(jobPanel);

		// Result Panel
		JPanel resultPanel = new JPanel(new BorderLayout());
		resultPanel.setBorder(BorderFactory.createTitledBorder("Analysis Results"));
		resultPane = new JTextPane();
		resultPane.setEditable(false);
		resultPane.setFont(new Font("Consolas", Font.PLAIN, 14));
		resultPanel.add(new JScrollPane(resultPane), BorderLayout.CENTER);
		centerPanel.add(resultPanel);

		add(centerPanel, BorderLayout.CENTER);

		// ===== BUTTON PANEL =====
		JPanel buttonPanel = new JPanel();

		JButton uploadButton = new JButton("Upload Resume (PDF)");
		uploadButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		uploadButton.addActionListener(e -> uploadPDF());
		buttonPanel.add(uploadButton);

		JButton analyzeButton = new JButton("Analyze Resume");
		analyzeButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		analyzeButton.addActionListener(e -> analyzeResume());
		buttonPanel.add(analyzeButton);

		add(buttonPanel, BorderLayout.SOUTH);
	}

	// Upload PDF
	private void uploadPDF() {
		JFileChooser chooser = new JFileChooser();
		int result = chooser.showOpenDialog(this);

		if (result == JFileChooser.APPROVE_OPTION) {
			selectedPDF = chooser.getSelectedFile();
			resultPane.setText("📄 Selected: " + selectedPDF.getName() + "\n");
		}
	}

	// Analyze Resume
	private void analyzeResume() {
		if (selectedPDF == null) {
			JOptionPane.showMessageDialog(this, "Please upload a PDF resume first!");
			return;
		}

		try {
			String resumeText = TestPDF.extractText(selectedPDF.getAbsolutePath());
			String jobDesc = jobDescArea.getText();

			// Parse Resume
			ResumeResult result = parseResume(resumeText, jobDesc);

			// Display results with colors
			resultPane.setText("");
			appendToPane(resultPane, "Candidate: " + result.candidateName, Color.BLACK);
			appendToPane(resultPane, "Experience: " + result.experience, Color.BLACK);
			appendToPane(resultPane,
					"Education: "
							+ (result.education.isEmpty() ? "Not Mentioned" : String.join(", ", result.education)),
					Color.BLACK);

			appendToPane(resultPane, "\nMatched Skills:", Color.GREEN);
			for (String skill : result.matchedSkills) {
				appendToPane(resultPane, "✓ " + skill, Color.GREEN);
			}

			appendToPane(resultPane, "\nMissing Skills:", Color.RED);
			for (String skill : result.missingSkills) {
				appendToPane(resultPane, "✗ " + skill, Color.RED);
			}

			appendToPane(resultPane, String.format("\nSkill Match Percentage: %.2f%%", result.matchPercent),
					Color.BLUE);

		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
		}
	}

	// Helper method to append colored text
	private void appendToPane(JTextPane tp, String msg, Color c) {
		javax.swing.text.StyleContext sc = javax.swing.text.StyleContext.getDefaultStyleContext();
		javax.swing.text.AttributeSet aset = sc.addAttribute(javax.swing.text.SimpleAttributeSet.EMPTY,
				javax.swing.text.StyleConstants.Foreground, c);

		aset = sc.addAttribute(aset, javax.swing.text.StyleConstants.FontFamily, "Consolas");
		aset = sc.addAttribute(aset, javax.swing.text.StyleConstants.FontSize, 14);

		int len = tp.getDocument().getLength();
		try {
			tp.getDocument().insertString(len, msg + "\n", aset);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ---------------- RESUME PARSER ----------------
	private ResumeResult parseResume(String text, String jobDescription) {
		ResumeResult result = new ResumeResult();

		// ---------- NAME ----------
		result.candidateName = "Unknown";
		try (InputStream modelIn = new FileInputStream("models/en-ner-person.bin")) {
			TokenNameFinderModel nameModel = new TokenNameFinderModel(modelIn);
			NameFinderME nameFinder = new NameFinderME(nameModel);
			String[] tokens = text.split("\\s+");
			Span[] spans = nameFinder.find(tokens);
			if (spans.length > 0) {
				StringBuilder sb = new StringBuilder();
				for (int i = spans[0].getStart(); i < spans[0].getEnd(); i++) {
					sb.append(tokens[i]).append(" ");
				}
				result.candidateName = sb.toString().trim();
			}
		} catch (Exception ignored) {
		}

		// Regex fallback
		if (result.candidateName.equals("Unknown") || result.candidateName.isEmpty()) {
			Matcher m = Pattern.compile("(?i)(name\\s*[:\\-]?\\s*)([A-Z][a-z]+\\s+[A-Z][a-z]+)").matcher(text);
			if (m.find()) {
				result.candidateName = m.group(2);
			} else {
				String firstLine = text.split("\\r?\\n")[0].trim();
				if (firstLine.split(" ").length <= 3 && firstLine.matches("[A-Za-z\\s]+")) {
					result.candidateName = firstLine;
				}
			}
		}

		// ---------- EXPERIENCE ----------
		result.experience = "Not Mentioned";
		Matcher expMatcher = Pattern.compile("(\\d+\\s+(years|year|months|month))", Pattern.CASE_INSENSITIVE)
				.matcher(text);
		if (expMatcher.find()) {
			result.experience = expMatcher.group(1);
		}

		// ---------- EDUCATION ----------
		result.education = new HashSet<>();
		String[] degrees = { "B\\.E", "B\\.Tech", "M\\.E", "M\\.Tech", "BSc", "MSc", "MBA", "PhD", "B\\.Com", "B\\.CA",
				"MCA" };
		for (String degree : degrees) {
			Matcher degMatcher = Pattern.compile("\\b" + degree + "\\b", Pattern.CASE_INSENSITIVE).matcher(text);
			if (degMatcher.find()) {
				result.education.add(degree.replace("\\.", "").toUpperCase());
			}
		}
		if (result.education.isEmpty()) {
			if (text.toLowerCase().contains("bachelor")) {
				result.education.add("BACHELOR");
			}
			if (text.toLowerCase().contains("master")) {
				result.education.add("MASTER");
			}
		}

		// ---------- SKILLS ----------
		String[] skillKeywords = { "java", "python", "html", "css", "javascript", "react", "sql", "node", "spring",
				"hibernate", "c++", "c#", "angular" };
		Set<String> resumeSkills = new HashSet<>();
		for (String skill : skillKeywords) {
			if (text.toLowerCase().contains(skill)) {
				resumeSkills.add(skill);
			}
		}

		// ---------- SKILL MATCH ----------
		result.matchedSkills = new HashSet<>();
		result.missingSkills = new HashSet<>();
		Set<String> jdSkills = new HashSet<>();
		for (String skill : skillKeywords) {
			if (jobDescription.toLowerCase().contains(skill)) {
				jdSkills.add(skill);
			}
		}

		// Matched: present in both resume & JD; Missing: in JD but not in resume
		for (String skill : jdSkills) {
			if (resumeSkills.contains(skill)) {
				result.matchedSkills.add(skill);
			} else {
				result.missingSkills.add(skill);
			}
		}

		result.matchPercent = jdSkills.isEmpty() ? 0 : (result.matchedSkills.size() * 100.0 / jdSkills.size());

		return result;
	}

	// Result container
	private static class ResumeResult {
		String candidateName;
		String experience;
		Set<String> education;
		Set<String> matchedSkills;
		Set<String> missingSkills;
		double matchPercent;
	}

	// Main
	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new ResumeAnalyzerGUI().setVisible(true));
	}
}

