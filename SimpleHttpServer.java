
import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Arrays;

import java.util.concurrent.CountDownLatch;

import java.util.ArrayList;

public class SimpleHttpServer {
    public static void main(String[] args) {
        int port = 8080; // If the 8080 port isn't available, try to use another port number.
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server listening on port " + port);
            UserService userService = new UserService(); // Create a single instance of UserService

            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Create a new thread to handle the client request
                Thread thread = new Thread(new ClientHandler(clientSocket, userService));
                thread.start();
            
                
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private UserService userService;
        private SearchService searchService;

        public ClientHandler(Socket clientSocket, UserService userService) {
            this.clientSocket = clientSocket;
            this.userService = userService;
            this.searchService = new SearchService();

        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                String request = in.readLine();
                System.out.println("Received request: " + request);

                // Extract the path from the request
                String[] requestParts = request.split(" ");
                String path = requestParts[1];
                System.out.println(path);

                // Read the HTML file and send it as the response
                if (path.equals("/")) {  // CASE 1: It sends index.html page to the client.
                    String filePath = "src/index.html";  // Default file to serve
                    File file = new File(filePath);  // Replace with the actual path to your HTML files
                    if (file.exists() && file.isFile()) {
                        System.out.println(file.getAbsolutePath());
                        String contentType = Files.probeContentType(Paths.get(filePath));
                        String content = new String(Files.readAllBytes(Paths.get(filePath)));

                        out.println("HTTP/1.1 200 OK");
                        out.println("Content-Type: " + contentType);
                        out.println("Content-Length: " + content.length());
                        out.println();
                        out.println(content);
                    } else {
                        // File not found
                        out.println("HTTP/1.1 404 Not Found");
                        out.println();
                    }
                }  else if (path.startsWith("/search")) {
                    // Handle search requests
                    String query = "";  // Extract the search query from the request, you need to implement this part
                    List<String> searchResults = searchService.search(query);
        
                    // Send the search results as the response
                    StringBuilder responseContent = new StringBuilder();
                    for (String result : searchResults) {
                        responseContent.append(result).append("\n");
                    }
        
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/plain");
                    out.println("Content-Length: " + responseContent.length());
                    out.println();
                    out.println(responseContent.toString());
                    
                }else {  // CASE 2: It sends a specific data to the client as an HTTP Response.
                    // ########################################################
                    // This #AREA# needs to be revised using an external class.
                    // For example, a UserService could handle requests related to users.
                    
                    String content = userService.handleRequest(path, request);
                    // ########################################################
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/plain");  // application/json would be OK!
                    out.println("Content-Length: " + content.length());
                    out.println();
                    out.println(content);
                }
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
