package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class CustomerServiceRestApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerServiceRestApplication.class, args);
	}
}
