package com.budget.budget.services;

import com.budget.budget.config.AppConfig;
import com.budget.budget.model.AddSpend;
import com.budget.budget.model.Participant;
import com.budget.budget.model.UserData;
import com.budget.budget.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserServices {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AppConfig appConfig;

    // Helper to sanitize table name


    public boolean createUserTable(String username) {
        try {
            String tableName = username;
            String createTableSQL = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    + "spendAmt VARCHAR(255), "
                    + "place VARCHAR(255), "
                    + "category VARCHAR(50), "
                    + "payeruser VARCHAR(50), "
                    + "payerbill VARCHAR(6), "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ")";
            jdbcTemplate.execute(createTableSQL);
            return true;
        } catch (Exception e) {
            System.out.println("Error creating table: " + e.getMessage());
            return false;
        }
    }

    public boolean updatePendingPayments(String username, String payerName, String category, String place, double spendAmt) {
        String tableName = username;
        // Encrypt values for comparison
        String encryptedPayerName = appConfig.encryptString(payerName);
        String encryptedCategory = appConfig.encryptString(category);
        String encryptedPlace = appConfig.encryptString(place);
        String encryptedSpendAmt = appConfig.encryptAmount(String.valueOf(spendAmt));

        String sql = "UPDATE " + tableName + " SET payerbill = 'True' " +
                "WHERE payeruser = ? AND category = ? AND place = ? AND spendAmt = ?";

        try {
            int rowsUpdated = jdbcTemplate.update(sql, encryptedPayerName, encryptedCategory, encryptedPlace, encryptedSpendAmt);
            return rowsUpdated > 0;
        } catch (Exception e) {
            System.out.println("Error updating pending payments: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> getPendingPayments(String username) {
        String tableName = username;
        if (!isValidUsername(tableName)) {
            System.out.println("Invalid username format");
            return new ArrayList<>();
        }

        try {
            String sql = "SELECT spendAmt, place, category, payeruser FROM " + tableName + " WHERE payerbill = 'False'";
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            System.out.println("Error fetching pending payments: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private boolean isValidUsername(String username) {
        return username.matches("^[a-zA-Z0-9_]+$");
    }

    public boolean addSpend(AddSpend addSpend, String username) {
        try {
            if (addSpend == null) {
                System.out.println("Error: AddSpend object is null");
                return false;
            }
            if (username == null || username.isEmpty()) {
                System.out.println("Error: Username is null or empty");
                return false;
            }
            String tableName = username;
            // Print values for debugging
            System.out.println("Adding spend for user: " + tableName);
            System.out.println("Amount: " + addSpend.getSpendAmt());
            System.out.println("Category: " + addSpend.getCategory());
            System.out.println("Place: " + addSpend.getPlace());
            String insertTableSQL = "INSERT INTO " + tableName + " (spendAmt, place, category, payeruser, payerbill) VALUES (?, ?, ?, ?, ?)";
            int rowsAffected = jdbcTemplate.update(
                insertTableSQL,
                appConfig.encryptAmount(addSpend.getSpendAmt()),
                appConfig.encryptString(addSpend.getPlace()),
                appConfig.encryptString(addSpend.getCategory()),
                "myself",
                "True"
            );
            return rowsAffected > 0;
        } catch (Exception e) {
            System.out.println("Error adding spend: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public double getTotalSpend(String username) {
        try {
            String tableName = username;
            String sql = "SELECT spendAmt FROM " + tableName + " WHERE payerbill = 'True'";
            List<String> encryptedAmounts = jdbcTemplate.queryForList(sql, String.class);
            double total = 0.0;
            for (String encryptedAmt : encryptedAmounts) {
                String decryptedAmt = appConfig.decryptAmount(encryptedAmt); // Use your decrypt method
                total += Double.parseDouble(decryptedAmt); // Parse and sum the decrypted values
            }
            return total;
        } catch (Exception e) {
            System.out.println("Error fetching total spend: " + e.getMessage());
            return 0.0;
        }
    }

    // New method to get user name
    public String getUserName(String username) {
        try {
            String email = appConfig.encryptEmail(username + "@gmail.com");
            String getNameSQL = "SELECT name FROM user_details WHERE email = ?";
            return jdbcTemplate.queryForObject(getNameSQL, new Object[]{email}, (rs, rowNum) -> {
                return appConfig.decryptName(rs.getString("name"));
            });
        } catch (Exception e) {
            System.out.println("Error fetching user name: " + e.getMessage());
            return "Unknown";
        }
    }

    public UserData authenticate(String email, String password) {
        return userRepository.findByEmailAndPassword(email, password).orElse(null);
    }

    public UserData findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public void saveUser(UserData userData) {
        userRepository.save(userData);
    }

    public boolean splitExpense(String payerUsername,String place,String category, double totalAmount, List<String> participants) {
        try {
            // Calculate each participant's share
            String splitUser = payerUsername.substring(0, payerUsername.indexOf('@'));
            double splitAmount = totalAmount / (participants.size() + 1);
            addUser(splitUser, splitAmount,place,category,"split = "+String.valueOf(participants.size()));

            // Update the balance for each participant except the payer
            for (String participantUsername : participants) {
                if (!participantUsername.equals(payerUsername)) {
                    String username = participantUsername.substring(0, participantUsername.indexOf('@'));
                    splitUser(username, splitAmount,place,category,splitUser,"False");
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("Error splitting expense: " + e.getMessage());
            return false;
        }
    }


    public boolean addUser(String username, double initialOwedAmount,String place,String category,String payerUser) {
        String insertUserSQL = "INSERT INTO "+username+" (spendAmt,place,category,payeruser,payerbill) VALUES (?,?,?,?,?)";

        try {
            int rowsAffected = jdbcTemplate.update(insertUserSQL, appConfig.encryptAmount(String.valueOf(initialOwedAmount)),appConfig.encryptName(place),appConfig.encryptString(category),appConfig.encryptName(payerUser),"True");
            return rowsAffected > 0; // Return true if the insert was successful
        } catch (Exception e) {
            System.out.println("Error adding user: " + e.getMessage());
            return false; // Return false in case of an error
        }
    }
    public boolean splitUser(String username, double initialOwedAmount,String place,String category,String payerUser, String bill) {
        String insertUserSQL = "INSERT INTO "+username+" (spendAmt,place,category,payeruser,payerbill) VALUES (?,?,?,?,?)";

        try {
            int rowsAffected = jdbcTemplate.update(insertUserSQL, appConfig.encryptAmount(String.valueOf(initialOwedAmount)),appConfig.encryptString(place),appConfig.encryptString(category),appConfig.encryptString(payerUser),bill);
            return rowsAffected > 0; // Return true if the insert was successful
        } catch (Exception e) {
            System.out.println("Error adding user: " + e.getMessage());
            return false; // Return false in case of an error
        }
    }

    public boolean updateIncomeDisplayPreference(String username, boolean showIncome) {
        try {
            if (username == null || username.isEmpty()) {
                System.out.println("Error: Username is null or empty");
                return false;
            }
            
            // Add email domain if not present
            if (!username.contains("@")) {
                username = username + "@gmail.com";
            }
            
            // Encrypt the email for database lookup
            String encryptedEmail = appConfig.encryptEmail(username);
            
            // Find the user
            UserData userData = userRepository.findByEmail(encryptedEmail).orElse(null);
            if (userData == null) {
                System.out.println("User not found with email: " + username);
                return false;
            }
            
            // Update the preference
            userData.setShowIncome(showIncome);
            userRepository.save(userData);
            
            System.out.println("Income display preference updated for user: " + username);
            return true;
        } catch (Exception e) {
            System.out.println("Error updating income display preference: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean getIncomeDisplayPreference(String username) {
        try {
            if (username == null || username.isEmpty()) {
                System.out.println("Error: Username is null or empty");
                return true; // Default to showing income
            }
            
            // Add email domain if not present
            if (!username.contains("@")) {
                username = username + "@gmail.com";
            }
            
            // Encrypt the email for database lookup
            String encryptedEmail = appConfig.encryptEmail(username);
            
            // Find the user
            UserData userData = userRepository.findByEmail(encryptedEmail).orElse(null);
            if (userData == null) {
                System.out.println("User not found with email: " + username);
                return true; // Default to showing income
            }
            
            return userData.isShowIncome();
        } catch (Exception e) {
            System.out.println("Error getting income display preference: " + e.getMessage());
            e.printStackTrace();
            return true; // Default to showing income on error
        }
    }

    public boolean updateMonthlyIncome(String username, double monthlyIncome) {
        try {
            if (username == null || username.isEmpty()) {
                System.out.println("Error: Username is null or empty");
                return false;
            }
            
            // Add email domain if not present
            if (!username.contains("@")) {
                username = username + "@gmail.com";
            }
            
            // Encrypt the email for database lookup
            String encryptedEmail = appConfig.encryptEmail(username);
            
            // Find the user
            UserData userData = userRepository.findByEmail(encryptedEmail).orElse(null);
            if (userData == null) {
                System.out.println("User not found with email: " + username);
                return false;
            }
            
            // Update the monthly income
            userData.setMonthlyIncome(monthlyIncome);
            userRepository.save(userData);
            
            System.out.println("Monthly income updated for user: " + username);
            return true;
        } catch (Exception e) {
            System.out.println("Error updating monthly income: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Double getMonthlyIncome(String username) {
        try {
            if (username == null || username.isEmpty()) {
                System.out.println("Error: Username is null or empty");
                return null; // No default value
            }
            if (!username.contains("@")) {
                username = username + "@gmail.com";
            }
            String encryptedEmail = appConfig.encryptEmail(username);
            UserData userData = userRepository.findByEmail(encryptedEmail).orElse(null);
            if (userData == null) {
                System.out.println("User not found with email: " + username);
                return null;
            }
            return userData.getMonthlyIncome();
        } catch (Exception e) {
            System.out.println("Error getting monthly income: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getUserSuggestionsByEmail(String emailPart) {
        // Return decrypted emails that match the search
        List<UserData> users = userRepository.findByEmailContaining(appConfig.encryptEmail(emailPart));
        List<String> suggestions = new ArrayList<>();
        for (UserData user : users) {
            suggestions.add(appConfig.decryptEmail(user.getEmail()));
        }
        return suggestions;
    }

}
