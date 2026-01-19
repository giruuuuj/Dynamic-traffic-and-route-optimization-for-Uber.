import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleTrafficServer {
    private static final int PORT = 8080;
    private static Map<String, Object> trafficData = new ConcurrentHashMap<>();
    
    public static void main(String[] args) throws IOException {
        System.out.println("🚀 Starting Uber Traffic Optimization Server...");
        System.out.println("📡 Server will be available at: http://localhost:" + PORT);
        System.out.println("⏹️  Press Ctrl+C to stop the server");
        System.out.println();
        
        // Initialize some sample data
        initializeSampleData();
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        }
    }
    
    private static void initializeSampleData() {
        Map<String, Object> traffic = new HashMap<>();
        traffic.put("overallCongestion", 0.35);
        traffic.put("averageSpeed", 42.5);
        traffic.put("incidents", 3);
        traffic.put("weatherImpact", 0.15);
        traffic.put("timestamp", new Date().toString());
        
        List<Map<String, Object>> incidents = new ArrayList<>();
        Map<String, Object> incident1 = new HashMap<>();
        incident1.put("id", 1);
        incident1.put("type", "ACCIDENT");
        incident1.put("location", "Highway 1");
        incident1.put("severity", "HIGH");
        incident1.put("latitude", 40.7128);
        incident1.put("longitude", -74.0060);
        incident1.put("description", "Multi-vehicle collision");
        incident1.put("timestamp", new Date().toString());
        
        Map<String, Object> incident2 = new HashMap<>();
        incident2.put("id", 2);
        incident2.put("type", "CONSTRUCTION");
        incident2.put("location", "Main St");
        incident2.put("severity", "MEDIUM");
        incident2.put("latitude", 40.7589);
        incident2.put("longitude", -73.9851);
        incident2.put("description", "Road construction work");
        incident2.put("timestamp", new Date().toString());
        
        incidents.add(incident1);
        incidents.add(incident2);
        traffic.put("incidents", incidents);
        
        trafficData.put("traffic", traffic);
    }
    
    private static class ClientHandler extends Thread {
        private final Socket clientSocket;
        
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        public void run() {
            try (
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
            ) {
                String inputLine;
                StringBuilder request = new StringBuilder();
                
                while ((inputLine = in.readLine()) != null && !inputLine.isEmpty()) {
                    request.append(inputLine).append("\r\n");
                }
                
                // Read the body if it's a POST request
                String requestStr = request.toString();
                if (requestStr.contains("POST")) {
                    int contentLength = getContentLength(requestStr);
                    if (contentLength > 0) {
                        char[] body = new char[contentLength];
                        in.read(body, 0, contentLength);
                        requestStr += new String(body);
                    }
                }
                
                String response = handleRequest(requestStr);
                out.println(response);
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            }
        }
        
        private int getContentLength(String request) {
            String[] lines = request.split("\r\n");
            for (String line : lines) {
                if (line.startsWith("Content-Length:")) {
                    return Integer.parseInt(line.split(":")[1].trim());
                }
            }
            return 0;
        }
        
        private String handleRequest(String request) {
            String[] lines = request.split("\r\n");
            if (lines.length == 0) return "HTTP/1.1 400 Bad Request\r\n\r\n";
            
            String firstLine = lines[0];
            String[] parts = firstLine.split(" ");
            
            if (parts.length < 2) return "HTTP/1.1 400 Bad Request\r\n\r\n";
            
            String method = parts[0];
            String path = parts[1];
            
            String jsonResponse = "";
            int statusCode = 200;
            
            if (path.equals("/api/v1/routes/health") && method.equals("GET")) {
                jsonResponse = "{\"status\":\"healthy\",\"service\":\"traffic-optimization\",\"timestamp\":\"" + new Date().toString() + "\"}";
            } else if (path.equals("/api/v1/routes/calculate") && method.equals("POST")) {
                jsonResponse = generateRouteResponse();
            } else if (path.equals("/api/v1/routes/alternatives") && method.equals("POST")) {
                jsonResponse = generateAlternativeRoutesResponse();
            } else if (path.equals("/api/v1/traffic/realtime") && method.equals("GET")) {
                jsonResponse = mapToJson(trafficData.get("traffic"));
            } else if (path.equals("/api/v1/analytics") && method.equals("GET")) {
                jsonResponse = generateAnalyticsResponse();
            } else {
                jsonResponse = "{\"error\":\"Not Found\"}";
                statusCode = 404;
            }
            
            return "HTTP/1.1 " + statusCode + " OK\r\n" +
                   "Content-Type: application/json\r\n" +
                   "Access-Control-Allow-Origin: *\r\n" +
                   "Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS\r\n" +
                   "Access-Control-Allow-Headers: Content-Type, Authorization\r\n" +
                   "Content-Length: " + jsonResponse.length() + "\r\n" +
                   "\r\n" +
                   jsonResponse;
        }
        
        private String generateRouteResponse() {
            return "{\"routeId\":\"" + UUID.randomUUID().toString() + "\"," +
                   "\"totalDistance\":5000.0," +
                   "\"totalTravelTime\":600.0," +
                   "\"totalCost\":15.50," +
                   "\"confidenceScore\":0.85," +
                   "\"routeType\":\"FASTEST\"," +
                   "\"calculatedAt\":\"" + new Date().toString() + "\"," +
                   "\"validUntil\":\"" + new Date(System.currentTimeMillis() + 900000).toString() + "\"," +
                   "\"trafficLights\":12," +
                   "\"tollRoads\":2," +
                   "\"averageSpeed\":30.0," +
                   "\"congestionLevel\":0.3," +
                   "\"weatherImpact\":0.1," +
                   "\"nodeIds\":[\"node_1\",\"node_2\",\"node_3\"]," +
                   "\"edgeIds\":[\"edge_1\",\"edge_2\"]," +
                   "\"status\":\"SUCCESS\"," +
                   "\"message\":\"Route calculated successfully\"," +
                   "\"processingTimeMs\":150}";
        }
        
        private String generateAlternativeRoutesResponse() {
            StringBuilder json = new StringBuilder("[");
            for (int i = 1; i <= 3; i++) {
                if (i > 1) json.append(",");
                json.append("{")
                   .append("\"routeId\":\"route_").append(UUID.randomUUID().toString()).append("\",")
                   .append("\"totalDistance\":").append(4000.0 + (i * 1000)).append(",")
                   .append("\"totalTravelTime\":").append(500.0 + (i * 120)).append(",")
                   .append("\"totalCost\":").append(12.0 + (i * 3)).append(",")
                   .append("\"routeType\":\"").append(i == 1 ? "FASTEST" : i == 2 ? "SHORTEST" : "ECONOMICAL").append("\",")
                   .append("\"averageSpeed\":").append(35.0 - (i * 5)).append(",")
                   .append("\"congestionLevel\":").append(0.2 + (i * 0.1))
                   .append("}");
            }
            json.append("]");
            return json.toString();
        }
        
        private String generateAnalyticsResponse() {
            return "{\"totalRoutesCalculated\":15420," +
                   "\"averageReduction\":18.5," +
                   "\"activeVehicles\":3421," +
                   "\"systemUptime\":\"99.98%\"," +
                   "\"predictionAccuracy\":87.3," +
                   "\"lastUpdated\":\"" + new Date().toString() + "\"}";
        }
        
        private String mapToJson(Object obj) {
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                StringBuilder json = new StringBuilder("{");
                boolean first = true;
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!first) json.append(",");
                    json.append("\"").append(entry.getKey()).append("\":");
                    if (entry.getValue() instanceof Map) {
                        json.append(mapToJson(entry.getValue()));
                    } else if (entry.getValue() instanceof List) {
                        json.append(listToJson((List<?>) entry.getValue()));
                    } else if (entry.getValue() instanceof String) {
                        json.append("\"").append(entry.getValue()).append("\"");
                    } else {
                        json.append(entry.getValue());
                    }
                    first = false;
                }
                json.append("}");
                return json.toString();
            }
            return "{}";
        }
        
        private String listToJson(List<?> list) {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) json.append(",");
                Object item = list.get(i);
                if (item instanceof Map) {
                    json.append(mapToJson(item));
                } else if (item instanceof String) {
                    json.append("\"").append(item).append("\"");
                } else {
                    json.append(item);
                }
            }
            json.append("]");
            return json.toString();
        }
    }
}
