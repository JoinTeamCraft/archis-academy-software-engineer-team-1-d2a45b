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

    default Optional<User> findByEmail(String email) {
        return findByEmailIgnoreCase(email);
    }

    /**
     * Checks if a user already exists with the given email address (case-insensitive).
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Finds a user by username (case-insensitive).
     */
    Optional<User> findByUsernameIgnoreCase(String username);

    default Optional<User> findByUsername(String username) {
        return findByUsernameIgnoreCase(username);
    }

    /**
     * Checks if a user already exists with the given username (case-insensitive).
     */
    boolean existsByUsernameIgnoreCase(String username);

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

    /**
     * Finds users within optional date filters for activity reporting.
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT u FROM User u
        WHERE (:startDate IS NULL OR u.createdAt >= :startDate)
          AND (:endDate IS NULL OR u.createdAt <= :endDate)
        ORDER BY u.id ASC
    """)
    List<User> findUsersForReport(
        @org.springframework.data.repository.query.Param("startDate") java.time.Instant startDate,
        @org.springframework.data.repository.query.Param("endDate") java.time.Instant endDate
    );
}
