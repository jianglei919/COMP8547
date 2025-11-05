package lab8;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class UWCrawler {

    private static final String BASE_URL = "https://www.uwindsor.ca";
    private static final String SAVE_DIRECTORY = "saved_pages";
    private static final int MAX_PAGES = 100;  // Limit to avoid overloading the server

    private Set<String> visitedPages = new HashSet<>();
    private Queue<String> pagesToVisit = new LinkedList<>();

    public static void main(String[] args) {
        UWCrawler crawler = new UWCrawler();
        crawler.crawl(BASE_URL);
    }

    public void crawl(String startUrl) {
        pagesToVisit.add(startUrl);
        while (!pagesToVisit.isEmpty() && visitedPages.size() < MAX_PAGES) {
            String url = pagesToVisit.poll();
            if (url != null && !visitedPages.contains(url)) {
                visitPage(url);
            }
        }
    }

    private void visitPage(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            System.out.println("Visiting: " + url);
            visitedPages.add(url);

            // Save the HTML content of the page to a file
            saveHtmlToFile(doc, url);

            // Extract and add links to other pages on the site
            Elements links = doc.select("a[href]");
            for (Element link : links) {
                String linkHref = link.absUrl("href");
                if (linkHref.startsWith(BASE_URL) && !visitedPages.contains(linkHref)) {
                    pagesToVisit.add(linkHref);
                }
            }
        } catch (IOException e) {
            System.err.println("Error accessing: " + url + " - " + e.getMessage());
        }
    }

    private void saveHtmlToFile(Document doc, String url) {
        try {
            // Create the directory if it doesn't exist
            File directory = new File(SAVE_DIRECTORY);
            if (!directory.exists()) {
                directory.mkdir();
            }

            // Generate a safe filename using the URL
            String safeFileName = Paths.get(url.replace("https://", "").replaceAll("[^a-zA-Z0-9.-]", "_")).getFileName().toString();
            File file = new File(SAVE_DIRECTORY + File.separator + safeFileName + ".html");

            // Save HTML content to a file
            FileWriter writer = new FileWriter(file);
            writer.write(doc.html());
            writer.close();

            System.out.println("Saved page to: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving file for URL: " + url + " - " + e.getMessage());
        }
    }
}
