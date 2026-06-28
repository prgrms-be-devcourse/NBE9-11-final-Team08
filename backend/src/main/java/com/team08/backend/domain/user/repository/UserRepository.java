package com.team08.backend.domain.user.repository;

import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(UserRole role);

    @Query("SELECT MAX(u.id) FROM User u")
    Optional<Long> findMaxId();
}
