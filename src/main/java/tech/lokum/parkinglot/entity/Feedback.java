package tech.lokum.parkinglot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * JPA entity representing feedback a user submitted about the application or service.
 */
@Entity
@Table(
    name = "feedback",
    indexes = {
        @Index(name = "idx_feedback_user_id", columnList = "user_id")
    }
)
public class Feedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "feedback_text", nullable = false, length = 1000)
    private String feedbackText;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    public Feedback() {
    }

    public Feedback(User user, String feedbackText, Integer rating) {
        this.user = user;
        this.feedbackText = feedbackText;
        this.rating = rating;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFeedbackText() {
        return feedbackText;
    }

    public void setFeedbackText(String feedbackText) {
        this.feedbackText = feedbackText;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}
