package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;

import br.ufsc.grad.renatoback.tcc.customer.service.rest.RemoteConfigWrapper.RemoteConfig;

@Service
public class CustomerService {

	private Log logger = LogFactory.getLog(CustomerService.class);

	private static final String LOYALTY_URL = "http://%s:8235/%d";
	private static final String POST_URL = "http://%s:8236/%d";
	private static final String EMAIL_URL = "http://%s:8237/%d";

	AtomicInteger counter = new AtomicInteger();

	@Autowired
	RemoteConfig remoteConfig;

	public void createCustomer() {
		_doProcessing();

		signalUserCreation(counter.incrementAndGet());
	}

	private void _doProcessing() {
		// TODO Adicionar algo que consuma tempo de processamento
	}

	private void signalUserCreation(int userId) {

		ListenableFuture<URI> loyaltyResult = new AsyncRestTemplate().postForLocation(
				String.format(LOYALTY_URL, remoteConfig.getLoyaltyServiceHost(), System.nanoTime()), null);
		logger.info("REQ >> Loyalty");
		ListenableFuture<URI> postResult = new AsyncRestTemplate()
				.postForLocation(String.format(POST_URL, remoteConfig.getPostServiceHost(), System.nanoTime()), null);
		logger.info("REQ >> Post");
		ListenableFuture<URI> emailResult = new AsyncRestTemplate()
				.postForLocation(String.format(EMAIL_URL, remoteConfig.getEmailServiceHost(), System.nanoTime()), null);
		logger.info("REQ >> Email");

		try {
			loyaltyResult.get(10, TimeUnit.SECONDS);
			logger.info("RES << Loyalty");
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.error("ERR -- Loyalty");
		}
		try {
			postResult.get(10, TimeUnit.SECONDS);
			logger.info("RES << Post");
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.error("ERR -- Post");
		}
		try {
			emailResult.get(10, TimeUnit.SECONDS);
			logger.info("RES << Email");
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			logger.error("ERR -- Email");
		}
	}

	public void createForAMinute() {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		long start = System.nanoTime();
		// System.out.printf("Nano %d and millis %d", start,
		// System.currentTimeMillis());
		// System.out.println();
		while (System.nanoTime() - start < 60000000000l) {
			executor.execute(() -> {
				createCustomer();
			});
		}
		int sobra = executor.shutdownNow().size();
		logger.info(String.format("Fim do processamento com %d threads não processadas.", sobra));
	}

}
