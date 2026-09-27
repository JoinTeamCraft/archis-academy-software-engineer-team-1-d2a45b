package tech.lokum.parkinglot.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.lokum.parkinglot.entity.PaymentStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long paymentId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String transactionReference;
}
