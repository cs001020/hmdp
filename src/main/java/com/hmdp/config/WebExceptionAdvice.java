package com.hmdp.config;

import com.hmdp.dto.Result;
import com.hmdp.exception.BaseException;
import com.hmdp.exception.ErrorReponse;
import com.hmdp.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import javax.servlet.http.HttpServletRequest;

/**
 * @author CHEN
 * @date 2022/10/07
 */
@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    //    @ExceptionHandler(RuntimeException.class)
//    public Result handleRuntimeException(RuntimeException e) {
//        log.error(e.toString(), e);
//        return Result.fail("服务器异常");
// 也可以将 BaseException 换为 RuntimeException
// 因为 RuntimeException 是 BaseException 的父类
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<?> handleAppException(BaseException ex, HttpServletRequest request) {
        ErrorReponse representation = new ErrorReponse(ex, request.getRequestURI());
        return new ResponseEntity<>(representation, new HttpHeaders(), ex.getError().getStatus());
    }

    @ExceptionHandler(value = ResourceNotFoundException.class)
    public ResponseEntity<ErrorReponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        ErrorReponse errorReponse = new ErrorReponse(ex, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorReponse);
    }

}
