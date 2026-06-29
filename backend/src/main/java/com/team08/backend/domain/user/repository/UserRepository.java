package com.team08.backend.domain.user.repository;

import com.team08.backend.domain.user.entity.User;
import com.team08.backend.domain.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(UserRole role);

    @Query("SELECT MAX(u.id) FROM User u")
    Optional<Long> findMaxId();

    @Query("""
            SELECT u.id
            FROM User u
            WHERE u.id IN :userIds
            """)
    List<Long> findExistingIds(@Param("userIds") List<Long> userIds);
}
