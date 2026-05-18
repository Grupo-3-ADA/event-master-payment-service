package com.eventmaster.paymentservice.infrastructure.web;

import com.eventmaster.paymentservice.domain.exceptions.PagamentoDuplicadoException;
import com.eventmaster.paymentservice.domain.exceptions.PagamentoNaoEncontradoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ManipuladorGlobalExcecao {

    @ExceptionHandler(PagamentoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleNaoEncontrado(PagamentoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(PagamentoDuplicadoException.class)
    public ResponseEntity<Map<String, String>> handleDuplicado(PagamentoDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "erro", ex.getMessage(),
                        "pagamentoId", ex.getPagamentoExistente().getId().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(Map.of("erro", mensagem));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("erro", ex.getMessage()));
    }
}
