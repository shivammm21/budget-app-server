package com.budget.budget.repository;

import com.budget.budget.model.UserData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserData,Long> {

    Optional<UserData> findByEmailAndPassword(String email, String password);

    Optional<UserData> findByEmail(String email);

    // Find users whose email contains a substring (for suggestions)
    List<UserData> findByEmailContaining(String emailPart);

    //void saveUser(UserData userData);
}
