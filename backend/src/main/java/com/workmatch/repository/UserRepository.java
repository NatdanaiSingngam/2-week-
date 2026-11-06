package com.workmatch.repository;

import com.workmatch.entity.Role;
import com.workmatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByRole(Role role);
    boolean existsByRole(Role role);
    List<User> findAllByOrderByCreatedAtDesc();
}
