package com.example.payment.service;

import com.example.payment.dto.BalanceDTO;
import com.example.payment.model.Balance;
import com.example.payment.repository.BalanceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @InjectMocks
    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetBalance_Found() {
        Long userId = 1L;
        Balance balance = Balance.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(1000))
                .build();

        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.of(balance));

        BalanceDTO result = balanceService.getBalance(userId);
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(BigDecimal.valueOf(1000), result.getAmount());
        verify(balanceRepository).findByUserId(userId);
    }

    @Test
    void testGetBalance_NotFound() {
        when(balanceRepository.findByUserId(2L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> balanceService.getBalance(2L));
    }

    @Test
    void testCreateBalance() {
        BalanceDTO newBalanceDTO = BalanceDTO.builder()
                .userId(10L)
                .amount(BigDecimal.valueOf(500))
                .build();

        Balance savedBalance = Balance.builder()
                .userId(newBalanceDTO.getUserId())
                .amount(newBalanceDTO.getAmount())
                .build();

        when(balanceRepository.save(any(Balance.class))).thenReturn(savedBalance);

        BalanceDTO result = balanceService.createBalance(newBalanceDTO);
        assertNotNull(result);
        assertEquals(10L, result.getUserId());
        assertEquals(BigDecimal.valueOf(500), result.getAmount());
        verify(balanceRepository).save(any(Balance.class));
    }

    @Test
    void testUpdateBalance_Found() {
        Long userId = 11L;
        Balance existing = Balance.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(100))
                .build();

        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(balanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BalanceDTO result = balanceService.updateBalance(userId, BigDecimal.valueOf(300));
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(BigDecimal.valueOf(300), result.getAmount());
        verify(balanceRepository).save(existing);
    }

    @Test
    void testUpdateBalance_NotFound() {
        when(balanceRepository.findByUserId(999L)).thenReturn(Optional.empty());

        Executable updateBalanceCall = () -> balanceService.updateBalance(999L, BigDecimal.valueOf(1234));

        assertThrows(EntityNotFoundException.class, updateBalanceCall);
    }

    @Test
    void testDeleteBalance_Found() {
        Long userId = 12L;
        Balance existing = Balance.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(100))
                .build();

        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        balanceService.deleteBalance(userId);
        verify(balanceRepository).delete(existing);
    }

    @Test
    void testDeleteBalance_NotFound() {
        when(balanceRepository.findByUserId(123L)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> balanceService.deleteBalance(123L));
    }

    @Test
    void testProcessPayment_Success() {
        Long userId = 15L;
        Balance existing = Balance.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(500))
                .build();

        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(balanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = balanceService.processPayment(userId, BigDecimal.valueOf(200));
        assertTrue(result);
        assertEquals(BigDecimal.valueOf(300), existing.getAmount());
        verify(balanceRepository).save(existing);
    }

    @Test
    void testProcessPayment_Failure() {
        Long userId = 16L;
        Balance existing = Balance.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(100))
                .build();

        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        boolean result = balanceService.processPayment(userId, BigDecimal.valueOf(200));
        assertFalse(result);
        assertEquals(BigDecimal.valueOf(100), existing.getAmount());
        verify(balanceRepository, never()).save(any());
    }

    @Test
    void testRefundPayment() {
        Long userId = 17L;
        Balance existing = Balance.builder()
                .userId(userId)
                .amount(BigDecimal.valueOf(100))
                .build();

        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
        when(balanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        balanceService.refundPayment(userId, BigDecimal.valueOf(50));
        assertEquals(BigDecimal.valueOf(150), existing.getAmount());
        verify(balanceRepository).save(existing);
    }

    @Test
    void testGetAllBalances() {
        List<Balance> list = new ArrayList<>();
        Balance b1 = Balance.builder().userId(1L).amount(BigDecimal.TEN).build();
        Balance b2 = Balance.builder().userId(2L).amount(BigDecimal.ONE).build();
        list.add(b1);
        list.add(b2);

        when(balanceRepository.findAll()).thenReturn(list);

        List<BalanceDTO> result = balanceService.getAllBalances();
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getUserId());
        assertEquals(BigDecimal.TEN, result.get(0).getAmount());
        assertEquals(2L, result.get(1).getUserId());
        assertEquals(BigDecimal.ONE, result.get(1).getAmount());
    }
}
