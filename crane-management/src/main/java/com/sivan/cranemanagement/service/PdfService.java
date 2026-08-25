package com.sivan.cranemanagement.service;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PdfService {

    private static final Duration PDF_TIMEOUT = Duration.ofSeconds(45);

    private final TemplateEngine templateEngine;

    public PdfService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] renderPdf(String templateName, Map<String, Object> variables, String baseUrl) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        context.setVariable("pdfDownload", true);

        String html = templateEngine.process(templateName, context)
                .replaceFirst("(?i)<head>", "<head><base href=\"" + baseUrl + "/\" />");

        Path htmlFile = null;
        Path pdfFile = null;
        Path userDataDir = null;
        try {
            htmlFile = Files.createTempFile("sivan-print-", ".html");
            pdfFile = Files.createTempFile("sivan-print-", ".pdf");
            userDataDir = Files.createTempDirectory("sivan-browser-");
            Files.writeString(htmlFile, html, StandardCharsets.UTF_8);

            Files.deleteIfExists(pdfFile);
            runBrowserPrint(htmlFile, pdfFile, userDataDir);
            return Files.readAllBytes(pdfFile);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to generate PDF", e);
        } finally {
            deleteQuietly(htmlFile);
            deleteQuietly(pdfFile);
            deleteQuietly(userDataDir);
        }
    }

    private void runBrowserPrint(Path htmlFile, Path pdfFile, Path userDataDir) throws IOException, InterruptedException {
        String browser = findBrowser();
        List<String> command = new ArrayList<>();
        command.add(browser);
        command.add("--headless=new");
        command.add("--disable-gpu");
        command.add("--no-sandbox");
        command.add("--user-data-dir=" + userDataDir.toAbsolutePath());
        command.add("--print-to-pdf=" + pdfFile.toAbsolutePath());
        command.add("--print-to-pdf-no-header");
        command.add(htmlFile.toUri().toString());

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        boolean finished = process.waitFor(PDF_TIMEOUT.toSeconds(), java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("PDF generation timed out");
        }
        if (process.exitValue() != 0 || !Files.exists(pdfFile) || Files.size(pdfFile) == 0) {
            throw new IllegalStateException("Browser could not generate PDF");
        }
    }

    private String findBrowser() {
        List<String> candidates = List.of(
                "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                "C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe",
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                "msedge",
                "chrome"
        );
        for (String candidate : candidates) {
            if (candidate.contains(":") && !Files.exists(Path.of(candidate))) {
                continue;
            }
            return candidate;
        }
        throw new IllegalStateException("Microsoft Edge or Google Chrome is required to generate PDF downloads");
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            if (Files.isDirectory(path)) {
                try (var paths = Files.walk(path)) {
                    paths.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
                }
            } else {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
        }
    }
}
