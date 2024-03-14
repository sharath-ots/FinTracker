package main.resources.java;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpenseTracker {
    private static List<Expense> expenses = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new ExpenseHandler());
        server.start();
    }

    static class ExpenseHandler implements HttpHandler {
      
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            System.out.println(method);
            if ("GET".equals(method)) {
                handleGetRequest(exchange);
            } else if ("POST".equals(method)) {
                handlePostRequest(exchange);
            }
        }
        
        private void handleGetRequest(HttpExchange exchange) throws IOException {
            String response = readHtmlFile("index.html");
            sendResponse(exchange, response);
        }
        
        private void handlePostRequest(HttpExchange exchange) throws IOException {
            String formData = new String(exchange.getRequestBody().readAllBytes());
            String[] pairs = formData.split("&");

            double amount = 0;
            String details = "";
            String dateString = "";
            String type = "";

            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                String key = keyValue[0];
                String value = keyValue[1];

                if ("amount".equals(key)) {
                    amount = Double.parseDouble(value);
                } else if ("details".equals(key)) {
                    details = value;
                } else if ("date".equals(key)) {
                    dateString = value;
                } else if ("type".equals(key)) {
                    type = value;
                }
            }

            Date date;
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                date = dateFormat.parse(dateString);
            } catch (ParseException e) {
                throw new IOException("Error date:" + e.getMessage());
            }

                Expense expense = new Expense(amount, details, date, type);
                expenses.add(expense);
           

            String response = readHtmlFile("index.html");
            response = response.replace("<!-- Add Expense -->", getExpenseRows());
            sendResponse(exchange, response);
        }

        private String readHtmlFile(String filename) throws IOException {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(filename);
            if (inputStream != null) {
                InputStreamReader isr = new InputStreamReader(inputStream);
                BufferedReader br = new BufferedReader(isr);

                StringBuilder content = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    content.append(line).append("\n");
                }

                return content.toString();
            } else {
                return "Error: Could not read HTML file";
            }
        }

        private void sendResponse(HttpExchange exchange, String response) throws IOException {
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(response);
            osw.flush();
            osw.close();
            os.close();
        }

        private String getExpenseRows() {
        	 StringBuilder rows = new StringBuilder();

        	    // Map to store expenses grouped by date
        	    Map<String, List<Expense>> expensesByDate = new LinkedHashMap<>();

        	    // Group expenses by date
        	    for (Expense expense : expenses) {
        	        String dateKey = expense.getDate(); // Assuming date is represented as a string
        	        List<Expense> expensesOnDate = expensesByDate.getOrDefault(dateKey, new ArrayList<>());
        	        expensesOnDate.add(expense);
        	        expensesByDate.put(dateKey, expensesOnDate);
        	    }

        	    // Generate rows for each date
        	    for (Map.Entry<String, List<Expense>> entry : expensesByDate.entrySet()) {
        	        String date = entry.getKey();
        	        List<Expense> expensesOnDate = entry.getValue();

        	        rows.append("<tr><th colspan=\"4\">").append(date).append("</th></tr>");

        	        for (Expense expense : expensesOnDate) {
        	            String rowType = expense.getType().startsWith("expense") ? "<tr class=\"expense\">" : "<tr class=\"income\">";
        	            rows.append(rowType)
        	                .append("<td>").append(expense.getAmount()).append("</td>")
        	                .append("<td>").append(expense.getDetails()).append("</td>")
        	                .append("<td>").append(expense.getDate()).append("</td>")
        	                .append("<td>").append(expense.getType()).append("</td>")
        	                .append("</tr>");
        	        }
        	    }

        	    return rows.toString();
        }

       
    }

    static class Expense {
        private double amount;
        private String details;
        private Date date;
        private String type;

        public Expense(double amount, String details, Date date, String type) {
            this.amount = amount;
            this.details = details;
            this.date = date;
            this.type = type;
        }

        public double getAmount() {
            return amount;
        }

        public String getDetails() {
            return details;
        }

        public String getDate() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, yyyy");
            return dateFormat.format(date);
        }
        public String getType() {
            return type;
        }
    }
}
