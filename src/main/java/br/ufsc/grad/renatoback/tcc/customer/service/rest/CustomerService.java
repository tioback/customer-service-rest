package br.ufsc.grad.renatoback.tcc.customer.service.rest;

public interface CustomerService {

	void createCustomer();

	void createForAMinute(int repetitions, int interval, int threads, int sleep);

}
