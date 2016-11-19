package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongBinaryOperator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LatencyAspect {

	private Log logger = LogFactory.getLog(LatencyAspect.class);

	private AtomicInteger counter = new AtomicInteger();
	private AtomicLong average = new AtomicLong();

	@Around("execution(* SpringCustomerService.create_Customer(..))")
	public Object log(ProceedingJoinPoint pjp) throws Throwable {
		long init = System.nanoTime();
		counter.incrementAndGet();
		try {
			return pjp.proceed();
		} finally {
			average.accumulateAndGet(System.nanoTime() - init, new LongBinaryOperator() {
				@Override
				public long applyAsLong(long n, long m) {
					return (n + m) / (n == 0 || m == 0 ? 1 : 2);
				}
			});
		}
	}

	@Before("execution(* SpringCustomerService.printStatistics(..)) && args(threads,sleep)")
	public void print(JoinPoint jointPoint, int threads, int sleep) throws Throwable {
		// int threads = 0, sleep = 0;
		logger.info(String.format("[STAT]-[Thread][Pausa][Registros][Latência]:\t%d\t%d\t%d\t%d", threads, sleep,
				counter.getAndSet(0), average.getAndSet(0)));
	}

}
