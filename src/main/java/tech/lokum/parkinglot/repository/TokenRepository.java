package tech.lokum.parkinglot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import tech.lokum.parkinglot.entity.Token;

@EnableJpaRepositories
public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByTokenValue(String tokenValue);

    List<Token> findByUsername(String username);
}