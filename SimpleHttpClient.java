package src;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SimpleHttpClient {
    public static void main(String[] args) {
        // Base URL for user login
        String baseUrl = "http://localhost:8080/user/login";

        // Sample username and password
        String username = "test";
        String password = "password";

        try {
            // Create the request URL with the provided username and password
            String requestUrl = baseUrl + "?id=" + username + "&passwd=" + password;

            // Create a URL object from the request URL
            URL url = new URL(requestUrl);

            // Open a connection to the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the request method (GET by default)
            connection.setRequestMethod("GET");

            // Send the request and receive the response
            int responseCode = connection.getResponseCode();

            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Print the response
            System.out.println("Response Code: " + responseCode);
            System.out.println("Response Body:");
            System.out.println(response.toString());

            // Close the connection
            connection.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
