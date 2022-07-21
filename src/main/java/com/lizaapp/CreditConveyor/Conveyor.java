package com.lizaapp.CreditConveyor;

import DTO.CreditDTO;
import DTO.LoanApplicationRequestDTO;
import DTO.LoanOfferDTO;
import DTO.ScoringDataDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Credit offers")
@RestController
public class Conveyor {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoanService loanService;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Conveyor.class);

    @PostMapping(value = "/conveyor/offers", consumes = "application/json", produces = "application/json")
    @ResponseBody
    @Operation(
            summary = "Кредитные предложения",
            description = "Позволяет получить кредитные предложения на разных условиях"
    )
    public List<LoanOfferDTO> offers(
            @RequestBody LoanApplicationRequestDTO loanApplicationRequestDTO
    ) throws Exception {
        log.info("Credit request accepting");
        log.debug("Applicant's details " + objectMapper.writeValueAsString(loanApplicationRequestDTO));

        log.info("Creating credit offers with different conditions");
        return loanService.prescore(loanApplicationRequestDTO);
    }

    @PostMapping(value = "/conveyor/calculation", consumes = "application/json", produces = "application/json")
    @ResponseBody
    @Operation(
            summary = "Заявка на кредит",
            description = "Позволяет получить заявку на кредит"
    )
    public CreditDTO credit(
            @RequestBody ScoringDataDTO scoringDataDTO
    ) throws Exception {
        log.info("Credit request with full applicant's data accepting");
        log.debug("Applicant's details " + objectMapper.writeValueAsString(scoringDataDTO));

        log.info("Validation of applicant's data");
        loanService.validate(scoringDataDTO);

        return loanService.calculation(scoringDataDTO, scoringDataDTO.getIsInsuranceEnabled(), scoringDataDTO.getIsSalaryClient());
    }
}




