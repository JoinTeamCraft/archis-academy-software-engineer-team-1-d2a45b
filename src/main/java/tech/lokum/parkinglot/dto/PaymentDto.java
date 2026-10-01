package tech.lokum.parkinglot.dto;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.lokum.parkinglot.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

    private Long id;
    private Long reservationId;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime paidAt;
}