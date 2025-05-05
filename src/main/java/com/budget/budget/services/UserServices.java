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

    public boolean createUserTable(String username) {
        try {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS " + username + " ("
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

        // Create a dynamic SQL query to update only the specific entries with additional conditions
        String sql = "UPDATE " + username + " SET payerbill = 'True' " +
                "WHERE payeruser = ? AND category = ? AND place = ? AND spendAmt = ?";

        try {
            // Execute the update with the provided parameters
            int rowsUpdated = jdbcTemplate.update(sql, payerName, category, place, spendAmt);
            return rowsUpdated > 0; // Return true if at least one row was updated
        } catch (Exception e) {
            System.out.println("Error updating pending payments: " + e.getMessage());
            return false;
        }
    }



    public List<Map<String, Object>> getPendingPayments(String username) {
        if (!isValidUsername(username)) {
            System.out.println("Invalid username format");
            return new ArrayList<>();
        }

        try {
            String sql = "SELECT spendAmt, place, category, payeruser FROM " + username + " WHERE payerbill = 'False'";
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
            String insertTableSQL = "INSERT INTO " + username + " (spendAmt, place, category,payeruser, payerbill) VALUES ('" +
                    appConfig.encryptAmount(addSpend.getSpendAmt()) + "', '" +
                    appConfig.encryptString(addSpend.getPlace()) + "', '" +
                    appConfig.encryptString(addSpend.getCategory()) + "', '" +
                    "myself" + "', '" +
                    "True" + "');";
            jdbcTemplate.execute(insertTableSQL);
            return true;
        } catch (Exception e) {
            System.out.println("Error adding spend: " + e.getMessage());
            return false;
        }
    }

    public double getTotalSpend(String username) {
        try {
            String sql = "SELECT spendAmt FROM " + username + " WHERE payerbill = 'True'";
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


    public String[] getRemainingBalance(String username) {
        try {
            String email = appConfig.encryptEmail(username + "@gmail.com");
            String getBudgetSQL = "SELECT income, name FROM user_details WHERE email = ?";
            return jdbcTemplate.queryForObject(getBudgetSQL, new Object[]{email}, (rs, rowNum) -> {
                String budget = appConfig.decryptAmount(rs.getString("income"));
                String name = appConfig.decryptName(rs.getString("name"));
                double totalSpend = getTotalSpend(username);
                double remainingBalance = Double.parseDouble(budget) - totalSpend;
                totalSpend = Double.parseDouble(budget) - remainingBalance;
                return new String[]{String.valueOf(remainingBalance), name, String.valueOf(totalSpend)};
            });
        } catch (Exception e) {
            System.out.println("Error fetching remaining balance and name: " + e.getMessage());
            return new String[]{"0.0", "Unknown"};
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


}
