package DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class PaymentScheduleElement {
      private Integer number;
      private LocalDate date;
      private BigDecimal totalPayment;
      private BigDecimal interestPayment; //проценты
      private BigDecimal debtPayment;  //основной долг
      private BigDecimal remainingDebt; //остаток основного долга
}
