package com.positionbook.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs entry and successful completion time for application methods.
 */
@Aspect
@Component
public class LoggingAspect {

    /** Creates the method execution logging aspect. */
    public LoggingAspect() {
    }

    /**
     * Logs the duration of a method invocation without changing its result.
     *
     * @param joinPoint invocation being observed
     * @return the invoked method's result
     * @throws Throwable if the invoked method throws an exception
     */
    @Around("within(com.positionbook..*) && !within(com.positionbook.filter..*)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {

        final String methodName = joinPoint.getSignature().toShortString();
        final String className = joinPoint.getTarget().getClass().getSimpleName();

        final Logger logger = LoggerFactory.getLogger(joinPoint.getSignature().getDeclaringType());

        if (logger.isDebugEnabled()) {
            logger.debug("Entered into method: {} method of class {}", className, methodName);
        }

        long startTime = System.currentTimeMillis();
        // Proceed outside a finally block so failed calls are not logged as successful.
        Object result = joinPoint.proceed();
        long endTime = System.currentTimeMillis();

        if (logger.isDebugEnabled()) {
            logger.debug("Successfully executed {} method  of {} class in {} ms",
                    methodName, className, (endTime - startTime));
        }

        return result;
    }
}
