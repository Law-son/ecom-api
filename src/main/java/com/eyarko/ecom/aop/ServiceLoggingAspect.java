package com.eyarko.ecom.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceLoggingAspect {
    private static final Logger logger = LoggerFactory.getLogger(ServiceLoggingAspect.class);
    private static final String SERVICE_POINTCUT = "execution(* com.eyarko.ecom.service..*(..))";

    @Before(SERVICE_POINTCUT)
    public void logServiceStart(JoinPoint joinPoint) {
        logger.debug("Starting {}", joinPoint.getSignature());
    }

    @After(SERVICE_POINTCUT)
    public void logServiceEnd(JoinPoint joinPoint) {
        logger.debug("Finished {}", joinPoint.getSignature());
    }

    @Around(SERVICE_POINTCUT)
    public Object logServiceExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long duration = System.currentTimeMillis() - start;
            logger.debug("{} executed in {} ms", joinPoint.getSignature(), duration);
        }
    }
}


