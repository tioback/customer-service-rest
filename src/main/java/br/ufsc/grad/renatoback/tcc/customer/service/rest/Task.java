package br.ufsc.grad.renatoback.tcc.customer.service.rest;

public class Task implements Runnable {

	private final SpringCustomerService service;
	private final Long start;
	private final Long interval;
	private final Integer sleep;

	public Task(SpringCustomerService service, Long start, Long interval, Integer sleep) {
		this.service = service;
		this.start = start;
		this.interval = interval;
		this.sleep = sleep;
	}

	@Override
	public void run() {
		while (System.currentTimeMillis() - start < interval) {
			service.create_Customer();

			try {
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

}
