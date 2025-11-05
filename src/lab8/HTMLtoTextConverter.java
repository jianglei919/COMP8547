package lab8;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class HTMLtoTextConverter {

    private static final String HTML_DIRECTORY = "saved_pages";
    private static final String TEXT_DIRECTORY = "text_pages";

    public static void main(String[] args) {
        HTMLtoTextConverter converter = new HTMLtoTextConverter();
        converter.convertAllHtmlFiles();
    }

    public void convertAllHtmlFiles() {
        // Create the text directory if it doesn't exist
        File textDir = new File(TEXT_DIRECTORY);
        if (!textDir.exists()) {
            textDir.mkdir();
        }

        // Get all HTML files from the saved_pages directory
        File htmlDir = new File(HTML_DIRECTORY);
        File[] htmlFiles = htmlDir.listFiles((dir, name) -> name.endsWith(".html"));

        if (htmlFiles == null || htmlFiles.length == 0) {
            System.out.println("No HTML files found in " + HTML_DIRECTORY);
            return;
        }

        // Process each HTML file
        for (File htmlFile : htmlFiles) {
            try {
                convertHtmlToText(htmlFile);
            } catch (IOException e) {
                System.err.println("Error converting file: " + htmlFile.getName() + " - " + e.getMessage());
            }
        }
    }

    private void convertHtmlToText(File htmlFile) throws IOException {
        // Parse the HTML file and extract the text
        Document doc = Jsoup.parse(htmlFile, "UTF-8");
        String textContent = doc.text();

        // Define the output text file with the same name as the HTML file
        String outputFileName = htmlFile.getName().replace(".html", ".txt");
        File textFile = new File(TEXT_DIRECTORY + File.separator + outputFileName);

        // Write the text content to the output file
        try (FileWriter writer = new FileWriter(textFile)) {
            writer.write(textContent);
            System.out.println("Converted " + htmlFile.getName() + " to " + textFile.getAbsolutePath());
        }
    }
}
