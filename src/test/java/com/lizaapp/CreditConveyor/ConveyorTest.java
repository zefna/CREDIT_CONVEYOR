package com.lizaapp.CreditConveyor;

import DTO.LoanApplicationRequestDTO;
import DTO.LoanOfferDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@WebMvcTest(value = Conveyor.class)
public class ConveyorTest {
/*
    @Autowired
    ObjectMapper mapper;*/

    @Autowired
    private LoanService loanService;

    @Autowired
    private Conveyor conveyor;

    @Autowired
    private MockMvc mockMvc;

//    LoanApplicationRequestDTO mockLoanRequest = new LoanApplicationRequestDTO();

    @Test
    void SHOULD_test() {
        assertThat(2 * 2).isEqualTo(4);
    }

    @Test
    void SHOULD_do_prescore_ON_createLoanOfferDTO_call() throws Exception {

        //TODO Закончить тест, дописать в loanRequest параметры
        LoanApplicationRequestDTO loanRequest = new LoanApplicationRequestDTO();

        List<LoanOfferDTO> expected = List.of(loanService.makePreoffer(loanRequest, false, false),
                loanService.makePreoffer(loanRequest, false, true),
                loanService.makePreoffer(loanRequest, true, false),
                loanService.makePreoffer(loanRequest, true, true));

        when(loanService.prescore(loanRequest)).thenReturn(expected);

        List<LoanOfferDTO> actual = conveyor.offers(loanRequest);

        assertThat(actual).isEqualTo(expected);
    }

}

