package com.lingolearn.repository;

import com.lingolearn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByOpenid(String openid);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}