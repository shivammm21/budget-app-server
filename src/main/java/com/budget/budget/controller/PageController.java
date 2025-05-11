package com.budget.budget.controller;

import com.budget.budget.config.AppConfig;
import com.budget.budget.model.AddSpend;
import com.budget.budget.model.Participant;
import com.budget.budget.model.UserData;
import com.budget.budget.services.UserServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PageController {

    @Autowired
    UserServices userService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AppConfig appConfig;

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody UserData userData) {
        String email = userData.getEmail();
        String password = userData.getPassword();

        System.out.println(appConfig.encryptEmail(email));
        //System.out.println(appConfig.decryptEmail("e5w5ACap1u8qwmunZ4zipOx7crCUSRYLcGn2bOFQTEY="));

        UserData existingUser = userService.findByEmail(appConfig.encryptEmail(email));

        if (existingUser == null) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        if (passwordEncoder.matches(password, existingUser.getPassword())) {
            return new ResponseEntity<>("Login successful", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody UserData userData) {

        if (userService.findByEmail(userData.getEmail()) != null) {
            return new ResponseEntity<>("Email already exists", HttpStatus.BAD_REQUEST);
        }

        String budgetId = userData.getEmail().substring(0, userData.getEmail().indexOf('@'));

        userData.setBudgetId(appConfig.encryptUsername(budgetId+"@budget"));
        userData.setName(appConfig.encryptName(userData.getName()));
        userData.setEmail(appConfig.encryptEmail(userData.getEmail()));
        userData.setPassword(passwordEncoder.encode(userData.getPassword()));

        // Only set monthlyIncome if provided (not null)
        if (userData.getMonthlyIncome() == null) {
            userData.setMonthlyIncome(null); // Explicitly set to null if not provided
        }

        // Save mobile number if provided
        if (userData.getMobileNumber() != null) {
            userData.setMobileNumber(userData.getMobileNumber());
        }

        String email = userData.getEmail();
        //String username = email.substring(0, email.indexOf('@'));

        if (userService.createUserTable(budgetId)) {
            userService.saveUser(userData);
            return new ResponseEntity<>("User registered successfully. Your budget ID is "+appConfig.decryptUsername(userData.getBudgetId()), HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("Error creating user table", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/add-spend")
    public ResponseEntity<String> addSpend(@RequestBody AddSpend addSpend) {
        try {
            // Validate required fields
            if (addSpend.getUsername() == null || addSpend.getUsername().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Username is required");
            }
            
            if (addSpend.getSpendAmt() == null || addSpend.getSpendAmt().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Spend amount is required");
            }
            
            if (addSpend.getCategory() == null || addSpend.getCategory().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Category is required");
            }
            
            // Set default place if missing
            if (addSpend.getPlace() == null || addSpend.getPlace().isEmpty()) {
                addSpend.setPlace("Home");
            }
            
            // Add email suffix if not present
            String username = addSpend.getUsername();
            if (!username.contains("@")) {
                username = username + "@gmail.com";
                addSpend.setUsername(username);
            }
            
            // Extract username without domain for database operations
            String dbUsername = username.substring(0, username.indexOf('@'));
            
            boolean isAdded = userService.addSpend(addSpend, dbUsername);
            if (isAdded) {
                return ResponseEntity.ok("Spend added successfully.");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding spend.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Exception while adding spend: " + e.getMessage());
        }
    }

    @PostMapping("/split-expense")
    public ResponseEntity<String> splitExpense(@RequestBody Map<String, Object> requestData) {
        // Validate that all required fields are present
        if (!requestData.containsKey("payerUsername") || requestData.get("payerUsername") == null) {
            return new ResponseEntity<>("Missing 'payerUsername' in request data.", HttpStatus.BAD_REQUEST);
        }
        if (!requestData.containsKey("totalAmount") || requestData.get("totalAmount") == null) {
            return new ResponseEntity<>("Missing 'totalAmount' in request data.", HttpStatus.BAD_REQUEST);
        }
        if (!requestData.containsKey("participants") || requestData.get("participants") == null) {
            return new ResponseEntity<>("Missing 'participants' in request data.", HttpStatus.BAD_REQUEST);
        }
        System.out.println(requestData.containsKey("place"));
        System.out.println(requestData.containsKey("category"));
        try {
            String payerUsername = requestData.get("payerUsername").toString();
            double totalAmount = Double.parseDouble(requestData.get("totalAmount").toString());
            String place = requestData.get("place").toString();
            String category = requestData.get("category").toString();
            System.out.println(place);
            System.out.println(category);
            // Check that participants is a List
            Object participantsObj = requestData.get("participants");
            if (!(participantsObj instanceof List)) {
                return new ResponseEntity<>("Invalid format for 'participants'. Expected a list.", HttpStatus.BAD_REQUEST);
            }

            // Cast participants to List<String> safely
            List<String> participants = new ArrayList<>();
            for (Object participant : (List<?>) participantsObj) {
                if (participant instanceof String) {
                    participants.add((String) participant);
                } else {
                    return new ResponseEntity<>("Invalid format for 'participants'. Each participant must be a string.", HttpStatus.BAD_REQUEST);
                }
            }

            boolean isSplitSuccessful = userService.splitExpense(payerUsername, place,category, totalAmount, participants);
            if (isSplitSuccessful) {
                return new ResponseEntity<>("Expense split successfully.", HttpStatus.OK);
            } else {
                return new ResponseEntity<>("Error splitting expense.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (NumberFormatException e) {
            return new ResponseEntity<>("Invalid format for 'totalAmount'.", HttpStatus.BAD_REQUEST);
        } catch (ClassCastException e) {
            return new ResponseEntity<>("Invalid format for 'participants'. Expected a list of usernames.", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("An error occurred while splitting the expense.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @PostMapping("/pay-split")
    public ResponseEntity<Map<String, String>> paySplit(@RequestBody Map<String, Object> requestData) {
        String username = (String) requestData.get("username");
        String payerName = (String) requestData.get("payerName");
        String category = (String) requestData.get("category");
        String place = (String) requestData.get("place");
        double spendAmt = ((Number) requestData.get("spendAmt")).doubleValue();

        // Fetch budgetId for the username
        String email = username.contains("@") ? username : username + "@gmail.com";
        UserData user = userService.findByEmail(appConfig.encryptEmail(email));
        if (user == null) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        // Remove '@budget' if present
        String budgetId = appConfig.decryptUsername(user.getBudgetId());
        if (budgetId.endsWith("@budget")) {
            budgetId = budgetId.substring(0, budgetId.indexOf("@budget"));
        }

        // Call the updated service method with the budgetId
        boolean updateSuccessful = userService.updatePendingPayments(budgetId, payerName, category, place, spendAmt);

        Map<String, String> response = new HashMap<>();
        if (updateSuccessful) {
            response.put("message", "Payments updated successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "Failed to update payments.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    @GetMapping("/dashboard/{username}")
    public ResponseEntity<Map<String, Object>> paySplitSpend(@PathVariable String username) {
        String totalSpend = String.valueOf(userService.getTotalSpend(username));
        String name = userService.getUserName(username);
        boolean showIncome = userService.getIncomeDisplayPreference(username);
        Double monthlyIncome = userService.getMonthlyIncome(username);

        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("totalSpend", totalSpend);
        dashboardData.put("userName", name);
        dashboardData.put("showIncome", showIncome);

        // Only include monthlyIncome and remainingBalance if set
        if (showIncome && monthlyIncome != null) {
            dashboardData.put("monthlyIncome", String.valueOf(monthlyIncome));
            double remainingBalance = monthlyIncome - Double.parseDouble(totalSpend);
            dashboardData.put("remainingBalance", String.valueOf(remainingBalance));
        }

        List<Map<String, Object>> pendingPayments = userService.getPendingPayments(username);
        // Decrypt fields for each pending payment
        List<Map<String, Object>> decryptedPendingPayments = new ArrayList<>();
        for (Map<String, Object> payment : pendingPayments) {
            Map<String, Object> decrypted = new HashMap<>(payment);
            try {
                decrypted.put("spendAmt", appConfig.decryptAmount((String) payment.get("spendAmt")));
                decrypted.put("place", appConfig.decryptString((String) payment.get("place")));
                decrypted.put("category", appConfig.decryptString((String) payment.get("category")));
                decrypted.put("payeruser", appConfig.decryptString((String) payment.get("payeruser")));
            } catch (Exception e) {
                // handle error or log
            }
            decryptedPendingPayments.add(decrypted);
        }
        dashboardData.put("pendingPayments", decryptedPendingPayments);

        return ResponseEntity.ok(dashboardData);
    }




    @GetMapping("/dashboard/history/{username}")
    public ResponseEntity<List<Map<String, Object>>> getUserHistory(@PathVariable String username) {
        try {
            // Fetch budgetId for the username
            String email = username.contains("@") ? username : username + "@gmail.com";
            UserData user = userService.findByEmail(appConfig.encryptEmail(email));
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            String budgetId = appConfig.decryptUsername(user.getBudgetId());
            if (budgetId.endsWith("@budget")) {
                budgetId = budgetId.substring(0, budgetId.indexOf("@budget"));
            }
            String query = "SELECT * FROM " + budgetId + " WHERE payerbill = 'True' ORDER BY created_at DESC";

            List<Map<String, Object>> history = new ArrayList<>();
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);

            for (Map<String, Object> row : results) {
                try {
                    Map<String, Object> decryptedRow = new HashMap<>(row);

                    // Decrypt fields with null checks
                    String spendAmt = (String) row.get("spendAmt");
                    String place = (String) row.get("place");
                    String category = (String) row.get("category");

                    if (spendAmt != null) {
                        try {
                            decryptedRow.put("spendAmt", appConfig.decryptAmount(spendAmt));
                        } catch (Exception e) {
                            System.out.println("Error decrypting spendAmt: " + e.getMessage());
                            decryptedRow.put("spendAmt", "0");
                        }
                    }

                    if (place != null) {
                        try {
                            decryptedRow.put("place", appConfig.decryptString(place));
                        } catch (Exception e) {
                            System.out.println("Error decrypting place: " + e.getMessage());
                            decryptedRow.put("place", "Unknown");
                        }
                    }

                    if (category != null) {
                        try {
                            decryptedRow.put("category", appConfig.decryptString(category));
                        } catch (Exception e) {
                            System.out.println("Error decrypting category: " + e.getMessage());
                            decryptedRow.put("category", "Uncategorized");
                        }
                    }

                    // Fetch split users using encrypted values for comparison
                    try {
                        // Use the original encrypted values from the row for comparison
                        String splitSql = "SELECT payeruser, payerbill FROM " + budgetId +
                            " WHERE place = ? AND category = ? AND spendAmt = ?";
                        List<Map<String, Object>> splits = jdbcTemplate.queryForList(
                            splitSql,
                            row.get("place"),  // Use original encrypted place
                            row.get("category"),  // Use original encrypted category
                            row.get("spendAmt")   // Use original encrypted spendAmt
                        );
                        List<Map<String, String>> splitUsers = new ArrayList<>();
                        for (Map<String, Object> split : splits) {
                            String payeruser = (String) split.get("payeruser");
                            if (payeruser != null) {
                                try {
                                    // Decrypt the payeruser for display
                                    String decryptedPayeruser = appConfig.decryptString(payeruser);
                                    splitUsers.add(Map.of(
                                        "user", decryptedPayeruser,
                                        "status", (String) split.get("payerbill")
                                    ));
                                } catch (Exception e) {
                                    System.out.println("Error decrypting payeruser: " + e.getMessage());
                                    // Skip this split user if decryption fails
                                    continue;
                                }
                            }
                        }
                        decryptedRow.put("splitUsers", splitUsers);
                    } catch (Exception e) {
                        System.out.println("Error fetching split users: " + e.getMessage());
                        decryptedRow.put("splitUsers", new ArrayList<>());
                    }

                    history.add(decryptedRow);
                } catch (Exception e) {
                    System.out.println("Error processing row: " + e.getMessage());
                    // Continue with next row instead of failing completely
                    continue;
                }
            }

            return ResponseEntity.ok(history);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/toggle-income-display")
    public ResponseEntity<Map<String, Object>> toggleIncomeDisplay(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract username from request
            String username = (String) requestData.get("username");
            if (username == null || username.isEmpty()) {
                response.put("success", false);
                response.put("message", "Username is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Extract showIncome preference from request
            Boolean showIncome = (Boolean) requestData.get("showIncome");
            if (showIncome == null) {
                response.put("success", false);
                response.put("message", "showIncome preference is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Update user preference
            boolean updated = userService.updateIncomeDisplayPreference(username, showIncome);
            
            if (updated) {
                response.put("success", true);
                response.put("message", "Income display preference updated successfully");
                response.put("showIncome", showIncome);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to update income display preference");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/update-monthly-income")
    public ResponseEntity<Map<String, Object>> updateMonthlyIncome(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Extract username from request
            String username = (String) requestData.get("username");
            if (username == null || username.isEmpty()) {
                response.put("success", false);
                response.put("message", "Username is required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Extract monthlyIncome from request
            Double monthlyIncome = null;
            try {
                if (requestData.get("monthlyIncome") instanceof Number) {
                    monthlyIncome = ((Number) requestData.get("monthlyIncome")).doubleValue();
                } else if (requestData.get("monthlyIncome") instanceof String) {
                    monthlyIncome = Double.parseDouble((String) requestData.get("monthlyIncome"));
                }
            } catch (Exception e) {
                response.put("success", false);
                response.put("message", "Invalid monthly income format");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (monthlyIncome == null || monthlyIncome < 0) {
                response.put("success", false);
                response.put("message", "Monthly income must be a positive number");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Update user's monthly income
            boolean updated = userService.updateMonthlyIncome(username, monthlyIncome);
            
            if (updated) {
                response.put("success", true);
                response.put("message", "Monthly income updated successfully");
                response.put("monthlyIncome", monthlyIncome);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to update monthly income");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/user-suggestions")
    public ResponseEntity<List<String>> getUserSuggestions(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }
        List<String> suggestions = userService.getUserSuggestionsByEmail(query);
        // If query is a 10-digit number, also search by mobile number
        if (query.matches("\\d{10}")) {
            suggestions.addAll(userService.getUserSuggestionsByMobile(query));
        }
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/user-id-by-mobile")
    public ResponseEntity<String> getUserIdByMobile(@RequestParam("mobile") String mobile) {
        UserData user = userService.findUserByMobile(mobile);
        if (user != null) {
            // Return the email (decrypted)
            return ResponseEntity.ok(appConfig.decryptEmail(user.getEmail()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("");
        }
    }

    @GetMapping("/expense-analysis/{username}")
    public ResponseEntity<List<Map<String, Object>>> getExpenseAnalysis(@PathVariable String username) {
        // Fetch budgetId for the username
        String email = username.contains("@") ? username : username + "@gmail.com";
        UserData user = userService.findByEmail(appConfig.encryptEmail(email));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        String budgetId = appConfig.decryptUsername(user.getBudgetId());
        if (budgetId.endsWith("@budget")) {
            budgetId = budgetId.substring(0, budgetId.indexOf("@budget"));
        }
        String sql = "SELECT category, spendAmt FROM " + budgetId + " WHERE payerbill = 'True'";
        try {
            Map<String, Double> categoryTotals = new HashMap<>();
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                String category;
                double amount;
                try {
                    category = appConfig.decryptString((String) row.get("category"));
                } catch (Exception e) {
                    category = "Unknown";
                }
                try {
                    amount = Double.parseDouble(appConfig.decryptAmount((String) row.get("spendAmt")));
                } catch (Exception e) {
                    amount = 0.0;
                }
                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
            }
            List<Map<String, Object>> results = new ArrayList<>();
            for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
                Map<String, Object> data = new HashMap<>();
                data.put("category", entry.getKey());
                data.put("amount", entry.getValue());
                results.add(data);
            }
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/reset-user-data")
    public ResponseEntity<Map<String, Object>> resetUserData(@RequestBody Map<String, Object> requestData) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Validate request
            if (!requestData.containsKey("username") || requestData.get("username") == null) {
                response.put("success", false);
                response.put("message", "Username is required");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
            
            String username = (String) requestData.get("username");
            
            // Extract username without domain for database operations if it contains @
            if (username.contains("@")) {
                username = username.substring(0, username.indexOf('@'));
            }
            
            boolean isReset = userService.resetUserData(username);
            
            if (isReset) {
                response.put("success", true);
                response.put("message", "User data has been reset successfully");
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                response.put("success", false);
                response.put("message", "Failed to reset user data");
                return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "An error occurred: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
