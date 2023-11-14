import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class SearchService {
    private static final String API_KEY = ""; // Replace with your Google API key
    private static final String SEARCH_ENGINE_ID = ""; // Replace with your Search Engine ID

    public List<String> search(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = "https://www.googleapis.com/customsearch/v1?key=" + API_KEY +
                    "&cx=" + SEARCH_ENGINE_ID + "&q=" + encodedQuery;

            URL requestUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            System.out.println("Search Response Code: " + responseCode);
            System.out.println("Search Response Body:");
            System.out.println(response.toString());

            List<String> searchResults = parseSearchResults(response.toString());
            System.out.println("Search Results:");
            for (String result : searchResults) {
                System.out.println(result);
            }

            return searchResults;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(); // Return an empty list if there are no search results or an error occurred
    }

    private List<String> parseSearchResults(String response) {
        List<String> searchResults = new ArrayList<>();
        int currentIndex = 0;
        for (int i = 0; i < 3; i++) {
            int titleStartIndex = response.indexOf("\"title\": \"", currentIndex);
            int titleEndIndex = response.indexOf("\",", titleStartIndex);
            if (titleStartIndex != -1 && titleEndIndex != -1) {
                String title = response.substring(titleStartIndex + 10, titleEndIndex);
                int linkStartIndex = response.indexOf("\"link\": \"", titleEndIndex);
                int linkEndIndex = response.indexOf("\",", linkStartIndex);
                if (linkStartIndex != -1 && linkEndIndex != -1) {
                    String link = response.substring(linkStartIndex + 9, linkEndIndex);
                    searchResults.add(title + " - " + link);
                    currentIndex = linkEndIndex;
                }
            }
        }
        return searchResults;
    }
    
}
