package com.example.evmonitorplatform.aspect;

import com.example.evmonitorplatform.common.utils.DateUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LogAspect {
    
    @Pointcut("execution(* com.example.evmonitorplatform.controller..*.*(..))")
    public void controllerPointcut() {}
    
    @Pointcut("execution(* com.example.evmonitorplatform.service..*.*(..))")
    public void servicePointcut() {}
    
    @Before("controllerPointcut()")
    public void doBeforeController(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            
            log.info("========================================== Start ==========================================");
            log.info("URL            : {}", request.getRequestURL());
            log.info("HTTP Method    : {}", request.getMethod());
            log.info("Class Method   : {}.{}", joinPoint.getSignature().getDeclaringTypeName(), joinPoint.getSignature().getName());
            log.info("IP             : {}", request.getRemoteAddr());
            log.info("Request Args   : {}", Arrays.toString(joinPoint.getArgs()));
        }
    }
    
    @AfterReturning(pointcut = "controllerPointcut()", returning = "result")
    public void doAfterReturning(Object result) {
        log.info("Response Result: {}", result);
        log.info("=========================================== End ===========================================");
    }
    
    @AfterThrowing(pointcut = "controllerPointcut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Throwable e) {
        log.error("Exception in {}.{}() with cause = {}", 
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                e.getCause() != null ? e.getCause() : "NULL");
        log.error("========================================= Error End =========================================");
    }
    
    @Before("servicePointcut()")
    public void doBeforeService(JoinPoint joinPoint) {
        log.info("Service Method : {}.{}", 
                joinPoint.getSignature().getDeclaringTypeName(), 
                joinPoint.getSignature().getName());
        log.info("Service Args   : {}", Arrays.toString(joinPoint.getArgs()));
    }
    
    @AfterReturning(pointcut = "servicePointcut()", returning = "result")
    public void doAfterServiceReturning(JoinPoint joinPoint, Object result) {
        log.info("Service Result : {}.{}() = {}", 
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                result);
    }
}