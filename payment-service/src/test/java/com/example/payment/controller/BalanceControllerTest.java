package com.example.payment.controller;

import com.example.payment.dto.BalanceDTO;
import com.example.payment.service.BalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BalanceControllerTest {

    @Mock
    private BalanceService balanceService;

    @InjectMocks
    private BalanceController balanceController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(balanceController).build();
    }

    @Test
    void getBalance_ShouldReturnBalance() throws Exception {
        Long userId = 1L;
        BalanceDTO balance = new BalanceDTO(userId, new BigDecimal("100.0"));

        when(balanceService.getBalance(userId)).thenReturn(balance);

        mockMvc.perform(get("/api/balances/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void getAllBalances_ShouldReturnList() throws Exception {
        List<BalanceDTO> balances = List.of(new BalanceDTO(1L, new BigDecimal("100.0")));
        when(balanceService.getAllBalances()).thenReturn(balances);

        mockMvc.perform(get("/api/balances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(1));
    }
}
