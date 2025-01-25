package com.example.payment.controller;

import com.example.payment.dto.BalanceDTO;
import com.example.payment.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/balances")
@RequiredArgsConstructor
@Tag(name = "Balance API", description = "API для управления балансом пользователей")
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN') or #userId == authentication.principal.userId")
    @Operation(summary = "Получить баланс пользователя")
    public ResponseEntity<BalanceDTO> getBalance(@PathVariable Long userId) {
        return ResponseEntity.ok(balanceService.getBalance(userId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Получить все балансы")
    public ResponseEntity<List<BalanceDTO>> getAllBalances() {
        return ResponseEntity.ok(balanceService.getAllBalances());
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Создать новый баланс")
    public ResponseEntity<BalanceDTO> createBalance(@RequestBody BalanceDTO balanceDTO) {
        return new ResponseEntity<>(balanceService.createBalance(balanceDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Обновить баланс пользователя")
    public ResponseEntity<BalanceDTO> updateBalance(
            @PathVariable Long userId,
            @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(balanceService.updateBalance(userId, amount));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Удалить баланс пользователя")
    public ResponseEntity<Void> deleteBalance(@PathVariable Long userId) {
        balanceService.deleteBalance(userId);
        return ResponseEntity.noContent().build();
    }
}
