package com.example.payment.service;

import com.example.payment.dto.BalanceDTO;
import com.example.payment.model.Balance;
import com.example.payment.repository.BalanceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;

    @Transactional(readOnly = true)
    public BalanceDTO getBalance(Long userId) {
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Balance not found for user: " + userId));
        return mapToDTO(balance);
    }

    @Transactional
    public BalanceDTO createBalance(BalanceDTO balanceDTO) {
        Balance balance = Balance.builder()
                .userId(balanceDTO.getUserId())
                .amount(balanceDTO.getAmount())
                .build();
        Balance savedBalance = balanceRepository.save(balance);
        return mapToDTO(savedBalance);
    }

    @Transactional
    public BalanceDTO updateBalance(Long userId, BigDecimal amount) {
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Balance not found for user: " + userId));
        balance.setAmount(amount);
        Balance updatedBalance = balanceRepository.save(balance);
        return mapToDTO(updatedBalance);
    }

    @Transactional
    public void deleteBalance(Long userId) {
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Balance not found for user: " + userId));
        balanceRepository.delete(balance);
    }


    @Transactional
    public boolean processPayment(Long userId, BigDecimal amount) {
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Balance not found for user: " + userId));
        
        if (balance.getAmount().compareTo(amount) >= 0) {
            balance.setAmount(balance.getAmount().subtract(amount));
            balanceRepository.save(balance);
            return true;
        }
        return false;
    }

    @Transactional
    public void refundPayment(Long userId, BigDecimal amount) {
        Balance balance = balanceRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Balance not found for user: " + userId));
        log.info("Refunding payment for user: {}, balance: {}, amount: {}", userId, balance.getAmount(), amount);
        balance.setAmount(balance.getAmount().add(amount));
        balanceRepository.save(balance);
    }

    @Transactional(readOnly = true)
    public List<BalanceDTO> getAllBalances() {
        return balanceRepository.findAll().stream()
                .map(this::mapToDTO)
                .toList();
    }

    private BalanceDTO mapToDTO(Balance balance) {
        return BalanceDTO.builder()
                .userId(balance.getUserId())
                .amount(balance.getAmount())
                .build();
    }
}
