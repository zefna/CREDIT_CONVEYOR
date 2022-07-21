package DTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ScoringDataDTO {
      private BigDecimal amount;
      private Integer term;
      private String firstName;
      private String lastName;
      private String middleName;
      private Gender gender;
      private LocalDate birthdate;
      private String passportSeries;
      private String passportNumber;
      private LocalDate passportIssueDate;
      private String passportIssueBranch;
      private MaritalStatus maritalStatus;
      private Integer dependentAmount;
      private EmploymentDTO employment;
      private String account;
      private Boolean isInsuranceEnabled;
      private Boolean isSalaryClient;

      public enum Gender {
            MALE, FEMALE, NONBINARY;
      }

      public enum MaritalStatus {
            MARRIED, DIVORCED
      }
}
