package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LatencyAspect {

	@Around("execution(* SpringCustomerService.create_Customer(..))")
	public Object log(ProceedingJoinPoint pjp) throws Throwable {
		System.err.println("logger 1");
		Object result = pjp.proceed();
		System.err.println("logger 2");
		return result;
	}

}
