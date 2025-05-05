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
        userData.setIncome(appConfig.encryptAmount(userData.getIncome()));

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


        boolean isAdded = userService.addSpend(addSpend, addSpend.getUsername().substring(0, addSpend.getUsername().indexOf('@')));
        if (isAdded) {
            return ResponseEntity.ok("Spend added successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding spend.");
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

        //List<Integer> entryIds = (List<Integer>) requestData.get("entryIds"); // IDs of entries to mark as paid
        String payerName = (String) requestData.get("payerName"); // Extract payerName from request
        String category = (String) requestData.get("category"); // Extract category from request
        String place = (String) requestData.get("place"); // Extract place from request
        double spendAmt = ((Number) requestData.get("spendAmt")).doubleValue(); // Extract spendAmt from request

        // Call the updated service method with the new parameters
        boolean updateSuccessful = userService.updatePendingPayments(username, payerName, category, place, spendAmt);

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
       // String totalSpend = String.valueOf(userService.getTotalSpend(username));

        String[] data = userService.getRemainingBalance(username);
        String remainingBalance = data[0];
        String name = data[1];
        String totalSpend = data[2];

        List<Map<String, Object>> pendingPayments = userService.getPendingPayments(username);

        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("totalSpend", totalSpend);
        dashboardData.put("remainingBalance", remainingBalance);
        dashboardData.put("userName", name);
        dashboardData.put("pendingPayments", pendingPayments);

        return ResponseEntity.ok(dashboardData);
    }




    @GetMapping("/dashboard/history/{username}")
    public ResponseEntity<List<Map<String, Object>>> getUserHistory(@PathVariable String username) {
        String query = "SELECT * FROM " + username + " WHERE payerbill = 'True'";

        List<Map<String, Object>> history = new ArrayList<>();

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(query);

            for (Map<String, Object> row : results) {
                Map<String, Object> decryptedRow = new HashMap<>(row);

                // Decrypt specific fields
                try {
                    decryptedRow.put("spendAmt", appConfig.decryptAmount((String) row.get("spendAmt")));
                    decryptedRow.put("place", appConfig.decryptString((String) row.get("place")));
                    decryptedRow.put("category", appConfig.decryptString((String) row.get("category")));
                } catch (Exception e) {
                    System.out.println("Error decrypting row: " + e.getMessage());
                }

                history.add(decryptedRow);
            }

            return ResponseEntity.ok(history);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}
