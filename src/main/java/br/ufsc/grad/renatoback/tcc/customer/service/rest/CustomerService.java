package br.ufsc.grad.renatoback.tcc.customer.service.rest;

public interface CustomerService {

	void createCustomer();

	void create_Customer();

	void printStatistics(int threads, int sleep);

	void createForAMinute(int repetitions, int interval, int threads, int sleep);

	void iterateCreateForAMinute(int repetitions, int interval, int threads, int start, int increment, int end);

}
