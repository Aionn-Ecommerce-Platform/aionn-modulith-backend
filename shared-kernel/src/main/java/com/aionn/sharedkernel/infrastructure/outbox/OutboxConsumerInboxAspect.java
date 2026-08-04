package com.aionn.sharedkernel.infrastructure.outbox;

import com.aionn.sharedkernel.domain.model.EventEnvelope;
import com.aionn.sharedkernel.integration.event.IntegrationEvent;
import java.lang.reflect.Method;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Gives every Spring event-listener its own durable inbox entry. A successful listener and its
 * marker commit atomically; if another listener fails, retrying the outbox event skips only the
 * listeners that already committed.
 */
@Aspect
@Component
@EnableAspectJAutoProxy(proxyTargetClass = true)
class OutboxConsumerInboxAspect {

    private final OutboxEventRepository repository;
    private final TransactionTemplate transactionTemplate;

    OutboxConsumerInboxAspect(OutboxEventRepository repository,
            PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Around("@annotation(org.springframework.context.event.EventListener)")
    Object consumeOnce(ProceedingJoinPoint joinPoint) throws Throwable {
        String eventId = eventId(joinPoint.getArgs());
        if (eventId == null) {
            return joinPoint.proceed();
        }

        String consumerId = consumerId(joinPoint);
        try {
            return transactionTemplate.execute(status -> {
                if (repository.wasProcessed(consumerId, eventId)) {
                    return null;
                }
                try {
                    Object result = joinPoint.proceed();
                    repository.markProcessed(consumerId, eventId);
                    return result;
                } catch (Throwable throwable) {
                    throw new ListenerInvocationException(throwable);
                }
            });
        } catch (ListenerInvocationException exception) {
            throw exception.getCause();
        }
    }

    private static String eventId(Object[] arguments) {
        if (arguments.length != 1) {
            return null;
        }
        if (arguments[0] instanceof IntegrationEvent event) {
            return event.eventId();
        }
        if (arguments[0] instanceof EventEnvelope envelope) {
            return envelope.eventId();
        }
        return null;
    }

    private static String consumerId(ProceedingJoinPoint joinPoint) {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?> targetType = AopUtils.getTargetClass(joinPoint.getTarget());
        Method specificMethod = AopUtils.getMostSpecificMethod(method, targetType);
        Class<?> parameterType = specificMethod.getParameterTypes()[0];
        return targetType.getName() + "#" + specificMethod.getName() + "(" + parameterType.getName() + ")";
    }

    private static final class ListenerInvocationException extends RuntimeException {
        private ListenerInvocationException(Throwable cause) {
            super(cause);
        }
    }
}
