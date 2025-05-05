package com.budget.budget.repository;

import com.budget.budget.model.UserData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserData,Long> {

    Optional<UserData> findByEmailAndPassword(String email, String password);

    Optional<UserData> findByEmail(String email);


    //void saveUser(UserData userData);
}
