import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map.Entry;

class UserService {
    private static final String DATA_DIR = "data/";
    private static final String USERS_FILE = DATA_DIR + "users.txt";
    private static final String DELETED_USERS_FILE = DATA_DIR + "deleted_users.txt";
    private static final String SEARCH_FILE = DATA_DIR + "search.txt"; // New constant for the search file
    private static final Object lock = new Object(); // Object used for synchronization
    private static List<String> users;
    private static List<String> deletedUsers;
    private static String currentLoggedInUser; // Variable to track the currently logged-in user
    private SearchService searchService; // Add a field of type SearchService


    public UserService() {
        users = loadUsersFromFile(USERS_FILE);
        deletedUsers = loadUsersFromFile(DELETED_USERS_FILE);
        currentLoggedInUser = null; // Initially no user is logged in
        searchService = new SearchService(); // Initialize the SearchService

    }

    public String handleRequest(String path, String request) {
        String[] parts = path.split("/");
        if (parts.length < 3) {
            return "Invalid request";
        }
    
        String[] queryParts = parts[2].split("\\?");
        String action = queryParts[0]; // Extract the action from the first part
        String userId = currentLoggedInUser != null ? currentLoggedInUser : "Unknown";
        logRequest(userId, request);
        switch (action) {
            case "join":
                return handleJoin(parts);
            case "login":
                return handleLogin(parts);
            case "logout":
                return handleLogout(parts);
            case "leave":
                return handleLeave(parts);
            case "recover":
                return handleRecover(parts);
            case "load_acc":
                return handleLoadAccounts(); 
            case "load_log":
                return handleLoadLogs(); 
            case "search":
                // Handle the "search" action
                return handleSearch(parts, queryParts[1]);    
            case "save_data":
                return handleSaveData(parts, queryParts[1]);
            case "load_data":
                return handleLoadData(parts);
            case "load_fri":
                return handleLoadFriendData(parts, queryParts[1]);
            case "load_hot": // Handle the "load_hot" action
                return handleLoadHot();
            default:
                return "Invalid action";
        }
    }
    private List<String> loadUsersFromFile(String filePath) {
        List<String> userList = new ArrayList<>();
        try {
            File file = new File(filePath);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    userList.add(line);
                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return userList;
    }
    private String handleLoadHot() {
        // Retrieve the hot search queries from the search file
        List<String> searchQueries = loadSearchQueries();

        // Sort the search queries by frequency in descending order
        Map<String, Integer> queryFrequencyMap = new HashMap<>();
        for (String query : searchQueries) {
            queryFrequencyMap.put(query, queryFrequencyMap.getOrDefault(query, 0) + 1);
        }
        List<Map.Entry<String, Integer>> sortedQueries = new ArrayList<>(queryFrequencyMap.entrySet());
        sortedQueries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Get the top 10 queries or less if there are fewer than 10
        int limit = Math.min(sortedQueries.size(), 10);
        List<String> topQueries = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            topQueries.add(sortedQueries.get(i).getKey());
        }

        // Create the response string
        StringBuilder responseContent = new StringBuilder();
        for (String query : topQueries) {
            responseContent.append(query).append("\n");
        }

        return responseContent.toString();
    }
    private List<String> loadSearchQueries() {
        List<String> searchQueries = new ArrayList<>();
        try {
            File file = new File(SEARCH_FILE);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    searchQueries.add(line);
                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return searchQueries;
    }
    
    private String handleLoadFriendData(String[] parts, String query) {
        if (!isLoggedIn()) {
            return "Access denied";
        }
        
        String[] queryParams = query.split("&");
        String friendId = null;
        for (String param : queryParams) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyValue[0].equalsIgnoreCase("q")) {
                friendId = keyValue[1];
                break;
            }
        }

        if (friendId == null) {
            return "Invalid friend ID";
        }
        // Load the search data from the friend's file
        StringBuilder searchData = new StringBuilder();
        try {
            File file = new File(DATA_DIR + friendId + "_search.txt");
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    searchData.append(line).append("\n");
                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        return searchData.toString();
    }
    
    private void saveUsersToFile(List<String> userList, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file);
            for (String user : userList) {
                writer.write(user + System.lineSeparator());
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String handleSearch(String[] parts, String query) {
        String[] queryParams = query.split("&");
        String searchQuery = "";
        for (String param : queryParams) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyValue[0].equalsIgnoreCase("q")) {
                searchQuery = keyValue[1];
                break;
            }
        }

        List<String> searchResults = searchService.search(searchQuery);

        saveSearchToFile(searchQuery);
        saveSearchDataForUser(currentLoggedInUser, searchQuery);

        StringBuilder responseContent = new StringBuilder();
        for (String result : searchResults) {
            responseContent.append(result).append("\n");
        }
        return responseContent.toString();
    }
    
    private void saveSearchToFile(String searchQuery) {
        try {
            File file = new File(SEARCH_FILE);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, true);
            writer.write(searchQuery + System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveSearchDataForUser(String username, String searchQuery) {
        try {
            File file = new File(DATA_DIR + username + "_search.txt");
            if (!file.exists()) {
                file.createNewFile();
            }
            FileWriter writer = new FileWriter(file, true);
            writer.write(searchQuery + System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private String handleLoadData(String[] parts) {
        if (!isLoggedIn()) {
            return "Access denied";
        }

        StringBuilder searchData = new StringBuilder();
        try {
            File file = new File(DATA_DIR + currentLoggedInUser + "_search.txt");
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    searchData.append(line).append("\n");
                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return searchData.toString();
    }

    private String handleSaveData(String[] parts, String query) {
        if (!isLoggedIn()) {
            return "Access denied";
        }

        String[] queryParams = query.split("&");
        String searchData = "";
        for (String param : queryParams) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyValue[0].equalsIgnoreCase("search")) {
                searchData = keyValue[1];
                break;
            }
        }

        saveSearchToFile(searchData);
        saveSearchDataForUser(currentLoggedInUser, searchData);

        return "Data saved successfully";
    }
    private boolean isLoggedIn() {
        return currentLoggedInUser != null;
    }
    private String handleJoin(String[] parts) {
        if (parts.length < 3) {
            return "Invalid request";
        }
    
        String[] splitQuery = parts[2].split("\\?", 2); 
        if (splitQuery.length < 2) {
            return "Invalid request";  // return error if no ? in the string
        }
    
        String query = splitQuery[1];
        String[] queryParams = query.split("&");
    
        String username = null;
        String password = null;
    
        for (String param : queryParams) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                if (key.equalsIgnoreCase("id")) {
                    username = value;
                } else if (key.equalsIgnoreCase("passwd")) {
                    password = value;
                }
            }
        }
    
        // Check if the username and password are present
        if (username == null || password == null) {
            return "Invalid username or password";
        }
    
        // Check if the username is already taken
        for (String user : users) {
            String[] userInfo = user.split(":");
            if (userInfo.length == 2 && userInfo[0].equals(username)) {
                return "Username already exists";
            }
        }
    
        // Validate the password
        if (password.length() < 4 || !Character.isLetter(password.charAt(0)) || password.contains("#")) {
            return "Invalid password";
        }
    
        // Add the user to the list
        String userEntry = username + ":" + password;
        users.add(userEntry);
        saveUsersToFile(users, USERS_FILE);
        logRequest(username, parts[0]);

        return "User joined successfully";
    }
    
    private void setCurrentLoggedInUser(String username) {
    
        currentLoggedInUser = username;
    
    }

    private String handleLogin(String[] parts) {
        if (parts.length < 3) {
            return "Invalid request";
        }
    
        String[] splitQuery = parts[2].split("\\?", 2);
        if (splitQuery.length < 2) {
            return "Invalid request";  // return error if no ? in the string
        }
    
        String query = splitQuery[1];
        String[] queryParams = query.split("&");
    
        String username = null;
        String password = null;
    
        for (String param : queryParams) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                if (key.equalsIgnoreCase("id")) {
                    username = value;
                } else if (key.equalsIgnoreCase("passwd")) {
                    password = value;
                }
            }
        }
    
        // Perform login verification
        if (username != null && password != null) {
            for (String user : users) {
                String[] userInfo = user.split(":");
                if (userInfo.length == 2 && userInfo[0].equals(username) && userInfo[1].equals(password)) {
                    setCurrentLoggedInUser(username); // Set the current logged-in user
                    logRequest(username, parts[0]);
                    return "Login successful";
                }
            }
        }
        return "Invalid username or password";
    }
    
    
    private String handleLogout(String[] parts) {
        
        if (parts.length < 3) {
            return "Invalid request";
        }
    
        String[] splitQuery = parts[2].split("\\?", 2);
        if (splitQuery.length < 2) {
            return "Invalid request";  // return error if no ? in the string
        }
    
        String query = splitQuery[1];
        String[] queryParams = query.split("&");
    
        String username = null;
    
        for (String param : queryParams) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                if (key.equalsIgnoreCase("id")) {
                    username = value;
                }
            }
        }

        logRequest(currentLoggedInUser, parts[0]);
        // Perform logout operation
        if (username != null) {
            if (username.equals("admin") || username.equals(currentLoggedInUser)) {
                setCurrentLoggedInUser(null); // Set the current logged-in user
                return "Logout successful";
            }
        }
    
        return "Invalid username";
    }
    
    
    private String handleLeave(String[] parts) {
        if (parts.length < 3) {
            return "Invalid request";
        }
    
        String[] splitQuery = parts[2].split("\\?", 2);
        if (splitQuery.length < 2) {
            return "Invalid request";  // return error if no ? in the string
        }
    
        String query = splitQuery[1];
        String[] queryParams = query.split("&");
    
        String username = null;
        String password = null;
    
        for (String param : queryParams) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                if (key.equalsIgnoreCase("id")) {
                    username = value;
                } else if (key.equalsIgnoreCase("passwd")) {
                    password = value;
                }
            }
        }
    
        logRequest(username, parts[0]);

        // Perform leave operation
        if (username != null && password != null) {
            String userEntry = username + ":" + password;
            if (users.contains(userEntry)) {
                if (password.equalsIgnoreCase("admin")) {
                    return "Leave not allowed for admin";
                } else {
                    users.remove(userEntry);
                    deletedUsers.add(userEntry);
                    saveUsersToFile(users, USERS_FILE);
                    saveUsersToFile(deletedUsers, DELETED_USERS_FILE);
                    return "Leave successful";
                }
            }
        }
    
        return "User not found";
    }
    
    private String handleRecover(String[] parts) {
        if (parts.length < 3) {
            return "Invalid request";
        }
    
        String[] splitQuery = parts[2].split("\\?", 2);
        if (splitQuery.length < 2) {
            return "Invalid request";  // return error if no ? in the string
        }
    
        String query = splitQuery[1];
        String[] queryParams = query.split("&");
    
        String username = null;
        String password = null;
    
        for (String param : queryParams) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2) {
                String key = keyValue[0];
                String value = keyValue[1];
                if (key.equalsIgnoreCase("id")) {
                    username = value;
                } else if (key.equalsIgnoreCase("passwd")) {
                    password = value;
                }
            }
        }
    
        // Check if the user exists in the deleted users list
        String userEntry = username + ":" + password;
        if (deletedUsers.contains(userEntry)) {
            // Remove the user from the deleted users list
            deletedUsers.remove(userEntry);
            saveUsersToFile(deletedUsers, DELETED_USERS_FILE);
    
            // Add the user back to the users list
            users.add(userEntry);
            saveUsersToFile(users, USERS_FILE);
    
            return "User recovery successful";
        } else {
            return "User not found in deleted users";
        }
    }
    
    private String handleLoadAccounts() {
        if (!isAdminLoggedIn()) {
            return "Access denied";
        }

        StringBuilder result = new StringBuilder();
        for (String user : users) {
            String[] userInfo = user.split(":");
            if (userInfo.length == 2) {
                result.append(userInfo[0]).append("\n");
            }
        }

        return result.toString();
    }
    
    private void logRequest(String userId, String request) {
        try {
            FileWriter writer = new FileWriter(DATA_DIR + userId + "_logs.txt", true);
            writer.write(request + System.lineSeparator());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    private String handleLoadLogs() {
        if (!isAdminLoggedIn()) {
            return "Access denied";
        }

        StringBuilder result = new StringBuilder();
        for (String user : users) {
            String[] userInfo = user.split(":");
            if (userInfo.length == 2) {
                String userId = userInfo[0];
                String userLogs = loadUserLogs(userId);
                result.append(userLogs);
            }
        }

        return result.toString();
    }
    private String loadUserLogs(String userId) {
        StringBuilder logs = new StringBuilder();
        try {
            File file = new File(DATA_DIR + userId + "_logs.txt");
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.append("[").append(userId).append("] ").append(line).append("\n");
                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logs.toString();
    }
    private boolean isAdminLoggedIn() {
        return currentLoggedInUser != null && currentLoggedInUser.equals("admin");
    }
}
