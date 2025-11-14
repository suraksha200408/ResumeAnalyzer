package com.resumeanalyzer;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

public class ResumeParser {

	public static void analyzeResume(String text, String jobDescription) {
		System.out.println("\n========= RESUME INSIGHTS =========");

		// ---------------- NAME DETECTION ----------------
		String candidateName = "Unknown";
		try (InputStream modelIn = new FileInputStream("models/en-ner-person.bin")) {
			TokenNameFinderModel nameFinderModel = new TokenNameFinderModel(modelIn);
			NameFinderME nameFinder = new NameFinderME(nameFinderModel);
			String[] tokens = text.split("\\s+");
			Span[] spans = nameFinder.find(tokens);
			if (spans.length > 0) {
				StringBuilder sb = new StringBuilder();
				for (int i = spans[0].getStart(); i < spans[0].getEnd(); i++) {
					sb.append(tokens[i]).append(" ");
				}
				candidateName = sb.toString().trim();
			}
		} catch (Exception e) {
			System.out.println("OpenNLP name detection failed: " + e.getMessage());
		}

		// Regex fallback: detects “Name: ...” or first line if formatted like a resume
		// header
		if (candidateName.equals("Unknown") || candidateName.isEmpty()) {
			Matcher m = Pattern.compile("(?i)(name\\s*[:\\-]?\\s*)([A-Z][a-z]+\\s+[A-Z][a-z]+)").matcher(text);
			if (m.find()) {
				candidateName = m.group(2);
			} else {
				// Try first line if it looks like a name
				String firstLine = text.split("\\r?\\n")[0].trim();
				if (firstLine.split(" ").length <= 3 && firstLine.matches("[A-Za-z\\s]+")) {
					candidateName = firstLine;
				}
			}
		}

		System.out.println("Candidate: " + candidateName);

		// ---------------- EXPERIENCE ----------------
		String experience = "Not Mentioned";
		Pattern expPattern = Pattern.compile("(\\d+\\s+(years|year|months|month))", Pattern.CASE_INSENSITIVE);
		Matcher expMatcher = expPattern.matcher(text);
		if (expMatcher.find()) {
			experience = expMatcher.group(1);
		}
		System.out.println("Experience: " + experience);

		// ---------------- EDUCATION ----------------
		Set<String> education = new HashSet<>();
		String[] degrees = { "B\\.E", "B\\.Tech", "M\\.E", "M\\.Tech", "BSc", "MSc", "MBA", "PhD", "B\\.Com", "B\\.CA",
				"MCA" };
		for (String degree : degrees) {
			Pattern p = Pattern.compile("\\b" + degree + "\\b", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(text);
			if (m.find()) {
				education.add(degree.replace("\\.", "").toUpperCase());
			}
		}

		if (education.isEmpty()) {
			if (text.toLowerCase().contains("bachelor")) {
				education.add("BACHELOR");
			}
			if (text.toLowerCase().contains("master")) {
				education.add("MASTER");
			}
		}

		if (education.isEmpty()) {
			System.out.println("Education: Not Mentioned");
		} else {
			System.out.println("Education: " + String.join(", ", education));
		}

		// ---------------- SKILLS ----------------
		Set<String> skills = new HashSet<>();
		String[] skillKeywords = { "java", "python", "html", "css", "javascript", "react", "sql", "node", "spring",
				"hibernate", "c++", "c#", "angular" };
		for (String skill : skillKeywords) {
			if (text.toLowerCase().contains(skill)) {
				skills.add(skill);
			}
		}

		// ---------------- SKILL MATCH CALCULATION ----------------
		if (jobDescription != null && !jobDescription.isEmpty()) {
			Set<String> matchedSkills = new HashSet<>();
			Set<String> missingSkills = new HashSet<>();

			// Detect which resume skills exist in JD
			for (String skill : skills) {
				if (jobDescription.toLowerCase().contains(skill)) {
					matchedSkills.add(skill);
				} else {
					missingSkills.add(skill);
				}
			}

			// Calculate match percentage
			Set<String> jdSkills = new HashSet<>();
			for (String skill : skillKeywords) {
				if (jobDescription.toLowerCase().contains(skill)) {
					jdSkills.add(skill);
				}
			}

			double matchPercent = 0;
			if (!jdSkills.isEmpty()) {
				matchPercent = (matchedSkills.size() * 100.0) / jdSkills.size();
			}

			// Print results
			System.out.println("Matched Skills: " + String.join(", ", matchedSkills));
			System.out.println("Missing Skills: " + String.join(", ", missingSkills));
			System.out.println(String.format("Skill Match Percentage: %.2f%%", matchPercent));

		} else {
			System.out.println("No Job Description provided. Showing all skills found:");
			System.out.println(String.join(", ", skills));
		}
	}
}



