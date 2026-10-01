package tech.lokum.parkinglot.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class nofication {

    @Id
    private Long id;
    private Long userId;
    private Long reservationId;
    private String type;
    private boolean status;
}
