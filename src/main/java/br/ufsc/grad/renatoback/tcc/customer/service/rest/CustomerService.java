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

@Service
public class CustomerService {

	private Log logger = LogFactory.getLog(CustomerService.class);

	private static final String LOYALTY_URL = "http://%s:%s/%d";
	private static final String POST_URL = "http://%s:%s/%d";
	private static final String EMAIL_URL = "http://%s:%s/%d";

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

		long time = System.nanoTime();

		ListenableFuture<URI> loyaltyResult = new AsyncRestTemplate().postForLocation(String.format(LOYALTY_URL,
				remoteConfig.getLoyaltyServiceHost(), remoteConfig.getLoyaltyServicePort(), time), null);
		// logger.info("REQ >> Loyalty");
		ListenableFuture<URI> postResult = new AsyncRestTemplate().postForLocation(
				String.format(POST_URL, remoteConfig.getPostServiceHost(), remoteConfig.getPostServicePort(), time),
				null);
		// logger.info("REQ >> Post");
		ListenableFuture<URI> emailResult = new AsyncRestTemplate().postForLocation(
				String.format(EMAIL_URL, remoteConfig.getEmailServiceHost(), remoteConfig.getEmailServicePort(), time),
				null);
		// logger.info("REQ >> Email");

		try {
			loyaltyResult.get(10, TimeUnit.SECONDS);
			// logger.info("RES << Loyalty");
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			// logger.error("ERR -- Loyalty");
		}
		try {
			postResult.get(10, TimeUnit.SECONDS);
			// logger.info("RES << Post");
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			// logger.error("ERR -- Post");
		}
		try {
			emailResult.get(10, TimeUnit.SECONDS);
			// logger.info("RES << Email");
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			// logger.error("ERR -- Email");
		}
	}

	public void createForAMinute(int repetitions, int interval_seg, int threads, int sleep) {
		// BEGIN CONFIG
		final long interval_nano = interval_seg * 1_000_000_000l;
		// END CONFIG

		ExecutorService executor;
		for (int i = 0; i < repetitions; i++) {
			logger.info(String.format("Starting batch %d", i));
			clearStatistics();

			executor = Executors.newFixedThreadPool(threads);
			long start = System.nanoTime();
			while (System.nanoTime() - start < interval_nano) {
				executor.execute(() -> {
					createCustomer();
				});

				try {
					Thread.sleep(sleep);
				} catch (InterruptedException e) {
				}
			}
			int sobra = executor.shutdownNow().size();
			logger.info(String.format("Fim do processamento com %d threads não processadas.", sobra));
			printStatistics();
		}
	}

	private void clearStatistics() {
		new AsyncRestTemplate().delete(String.format(LOYALTY_URL, remoteConfig.getLoyaltyServiceHost(),
				remoteConfig.getLoyaltyServicePort(), null));
		new AsyncRestTemplate().delete(
				String.format(POST_URL, remoteConfig.getPostServiceHost(), remoteConfig.getPostServicePort(), null));
		new AsyncRestTemplate().delete(
				String.format(EMAIL_URL, remoteConfig.getEmailServiceHost(), remoteConfig.getEmailServicePort(), null));
	}

	private void printStatistics() {
		new AsyncRestTemplate().put(String.format(LOYALTY_URL, remoteConfig.getLoyaltyServiceHost(),
				remoteConfig.getLoyaltyServicePort(), null), null);
		new AsyncRestTemplate().put(
				String.format(POST_URL, remoteConfig.getPostServiceHost(), remoteConfig.getPostServicePort(), null),
				null);
		new AsyncRestTemplate().put(
				String.format(EMAIL_URL, remoteConfig.getEmailServiceHost(), remoteConfig.getEmailServicePort(), null),
				null);
	}

}
