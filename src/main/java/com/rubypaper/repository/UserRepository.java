package com.rubypaper.repository;


import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rubypaper.domain.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserid(String userid);
    boolean existsByUserid(String userid);
    void deleteByUserid(String userid); // <- 이 메서드 추가

    void deleteUserById(Long id);

    void deleteUserByUserid(@NotBlank(message = "아이디는 필수 입니다.") String userid);

    @EntityGraph(attributePaths = "boards")
    Optional<User> findById (Long Userid);
}
