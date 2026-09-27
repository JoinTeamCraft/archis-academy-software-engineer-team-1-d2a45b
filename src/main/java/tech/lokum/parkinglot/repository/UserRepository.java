package tech.lokum.parkinglot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Finds a user by email address (case-insensitive).
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Checks if a user already exists with the given email address (case-insensitive).
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Retrieves all users with a specific role.
     */
    List<User> findByRole(Role role);

    /**
     * Retrieves all active users with a specific role.
     */
    List<User> findByRoleAndActiveTrue(Role role);

    /**
     * Finds an active user by ID.
     */
    Optional<User> findByIdAndActiveTrue(Long id);

    /**
     * Retrieves a paginated list of all active users.
     */
    Page<User> findByActiveTrue(Pageable pageable);
}
