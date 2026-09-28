package tech.lokum.parkinglot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByRoleAndActiveTrue(Role role);
}
