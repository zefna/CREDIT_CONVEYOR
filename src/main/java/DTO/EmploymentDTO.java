package DTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EmploymentDTO {
      private EmploymentStatus employmentStatus;
      private String employerINN;
      private BigDecimal salary;
      private Position position;
      private Integer workExperienceTotal;
      private Integer workExperienceCurrent;

      public enum EmploymentStatus {
            UNEMPLOYED, SELFEMPLOYED, BUSINESSOWNER;
      }

      public enum Position {
            MIDDLEMANAGER, TOPMANAGER;
      }
}
