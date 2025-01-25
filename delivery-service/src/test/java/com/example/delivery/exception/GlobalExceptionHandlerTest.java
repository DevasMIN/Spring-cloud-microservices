package com.example.delivery.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    private final WebRequest webRequest = mock(WebRequest.class);

    @Test
    void handleDeliveryNotFoundException() {
        when(webRequest.getDescription(false)).thenReturn("uri=/api/deliveries/1");
        DeliveryNotFoundException ex = new DeliveryNotFoundException("Доставка не найдена");
        
        ResponseEntity<Object> response = exceptionHandler.handleDeliveryNotFoundException(ex, webRequest);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Доставка не найдена", body.get("message"));
        assertEquals("Not Found", body.get("error"));
        assertEquals("/api/deliveries/1", body.get("path"));
        assertNotNull(body.get("timestamp"));
    }

    @Test
    void handleValidationExceptions() {
        // Подготовка моков
        when(webRequest.getDescription(false)).thenReturn("uri=/api/deliveries");
        
        MethodParameter parameter = mock(MethodParameter.class);
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);
        
        // Настройка ошибок валидации
        FieldError fieldError = new FieldError(
            "delivery",
            "deliveryAddress",
            "Адрес доставки обязателен"
        );
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        
        // Вызов тестируемого метода
        ResponseEntity<Object> response = exceptionHandler.handleValidationExceptions(ex, webRequest);
        
        // Проверки
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Validation Failed", body.get("error"));
        assertEquals("/api/deliveries", body.get("path"));
        assertNotNull(body.get("timestamp"));
        
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertNotNull(errors);
        assertEquals("Адрес доставки обязателен", errors.get("deliveryAddress"));
    }
}
