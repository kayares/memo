package com.kayares.memo.exception;

import com.kayares.memo.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(404).body(new ErrorResponse(404, e.getMessage()));
    }

    @ExceptionHandler(MemoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemoNotFound(MemoNotFoundException e) {
        return ResponseEntity.status(404).body(new ErrorResponse(404, e.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException e) {
        return ResponseEntity.status(403).body(new ErrorResponse(403, e.getMessage()));
    }
}