package tech.lokum.parkinglot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.lokum.parkinglot.entity.Feedback;

/**
 * Spring Data JPA repository for {@link Feedback} entities.
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
