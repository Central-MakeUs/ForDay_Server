package com.example.ForDay.domain.user.repository;

import com.example.ForDay.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findBySocialId(String socialId);
}
