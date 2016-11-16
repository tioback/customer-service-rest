package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerController {

	@Autowired
	CustomerService customerService;

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(code = HttpStatus.CREATED)
	public void create() {
		customerService.createCustomer();
	}

	/**
	 * Exemplo: GET /50/30/2/25 50 repetições por 30 segundos de intervalo
	 * executar 2 threads com 25 milisegundos de sleep
	 * 
	 * @param repetitions
	 * @param interval
	 * @param threads
	 * @param sleep
	 */
	@RequestMapping(path = "/{repetitions}/{interval}/{threads}/{sleep}", method = RequestMethod.GET)
	@ResponseStatus(code = HttpStatus.OK)
	public void createForAMinute(@PathVariable(required = true, name = "repetitions") int repetitions,
			@PathVariable(required = true, name = "interval") int interval,
			@PathVariable(required = true, name = "threads") int threads,
			@PathVariable(required = true, name = "sleep") int sleep) {
		customerService.createForAMinute(repetitions, interval, threads, sleep);
	}

	/**
	 * Exemplo: GET /50/30/2/25 50 repetições por 30 segundos de intervalo
	 * executar 2 threads com 25 milisegundos de sleep
	 * 
	 * @param repetitions
	 * @param interval
	 * @param threads
	 * @param sleep
	 */
	@RequestMapping(path = "/{repetitions}/{interval}/{threads}/{start}/{increment}/{end}", method = RequestMethod.GET)
	@ResponseStatus(code = HttpStatus.OK)
	public void createForAMinute(@PathVariable(required = true, name = "repetitions") int repetitions,
			@PathVariable(required = true, name = "interval") int interval,
			@PathVariable(required = true, name = "threads") int threads,
			@PathVariable(required = true, name = "start") int start,
			@PathVariable(required = true, name = "increment") int increment,
			@PathVariable(required = true, name = "end") int end) {
		customerService.iterateCreateForAMinute(repetitions, interval, threads, start, increment, end);
	}

	@RequestMapping(method = RequestMethod.OPTIONS)
	@ResponseStatus(code = HttpStatus.OK)
	public void printTime() {
		System.out.println("Nano time: " + System.nanoTime());
	}
}
