package com.example.memberplatform.error;

import java.util.Map;

import com.example.memberplatform.service.MemberService.BusinessException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Log logger = LogFactory.getLog(ApiExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Map<String, String>> handleBusinessException(BusinessException exception) {
        return ResponseEntity.status(exception.getStatus()).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, String>> handleInvalidJson() {
        return ResponseEntity.badRequest().body(Map.of("message", "輸入資料格式錯誤"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> handleUnexpected(Exception exception) {
        logger.error("未預期錯誤", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "系統發生錯誤，請稍後再試"));
    }
}
