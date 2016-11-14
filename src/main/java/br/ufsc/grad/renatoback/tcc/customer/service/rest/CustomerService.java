package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.AsyncRestTemplate;

@Service
public class CustomerService {

	private Log logger = LogFactory.getLog(CustomerService.class);

	private static final String LOYALTY_URL = "http://%s:%s/%d";
	private static final String POST_URL = "http://%s:%s/%d";
	private static final String EMAIL_URL = "http://%s:%s/%d";
	private static final String PRINT_LOYALTY_STATISTICS_URL = "http://%s:%s/%d/%d";
	private static final String PRINT_POST_STATISTICS_URL = "http://%s:%s/%d/%d";
	private static final String PRINT_EMAIL_STATISTICS_URL = "http://%s:%s/%d/%d";

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

	AtomicCyclicCounter index = new AtomicCyclicCounter(5);

	private void signalUserCreation(int userId) {

		long time = System.nanoTime();

		switch (index.cyclicallyIncrementAndGet()) {
		case 0:
			doLoyalty(time);
			doPost(time);
			doEmail(time);
			break;
		case 1:
			doLoyalty(time);
			doEmail(time);
			doPost(time);
			break;
		case 2:
			doPost(time);
			doLoyalty(time);
			doEmail(time);
			break;
		case 3:
			doPost(time);
			doEmail(time);
			doLoyalty(time);
			break;
		case 4:
			doEmail(time);
			doPost(time);
			doLoyalty(time);
			break;
		case 5:
			doEmail(time);
			doLoyalty(time);
			doPost(time);
			break;
		}
	}

	private void doLoyalty(long time) {
		new AsyncRestTemplate().postForLocation(String.format(LOYALTY_URL, remoteConfig.getLoyaltyServiceHost(),
				remoteConfig.getLoyaltyServicePort(), time), null);
		return;
	}

	private void doPost(long time) {
		new AsyncRestTemplate().postForLocation(
				String.format(POST_URL, remoteConfig.getPostServiceHost(), remoteConfig.getPostServicePort(), time),
				null);
		return;
	}

	private void doEmail(long time) {
		new AsyncRestTemplate().postForLocation(
				String.format(EMAIL_URL, remoteConfig.getEmailServiceHost(), remoteConfig.getEmailServicePort(), time),
				null);
		return;
	}

	public void createForAMinute(int repetitions, int interval_seg, int threads, int sleep) {
		// BEGIN CONFIG
		final long interval_nano = interval_seg * 1_000_000_000l;
		// END CONFIG

		ExecutorService executor;
		for (int i = 0; i < repetitions; i++) {
			logger.info(String.format("Starting batch %d", i));
			int activeThreadsCount = Thread.activeCount();
			System.err.println(String.format("Starting batch %d [%d]", i, activeThreadsCount));
			clearStatistics();

			executor = Executors.newFixedThreadPool(threads);
			long start = System.nanoTime();
			for (int j = 0; j < threads; j++) {
				executor.execute(() -> {
					while (System.nanoTime() - start < interval_nano) {
						createCustomer();

						try {
							Thread.sleep(sleep);
						} catch (InterruptedException e) {
						}
					}
				});
			}
			executor.shutdown();
			int sobra = 0;
			try {
				if (!executor.awaitTermination(interval_seg, TimeUnit.SECONDS)) {
					sobra = executor.shutdownNow().size();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.info(String.format("Fim do processamento com %d threads não processadas.", sobra));
			printStatistics(threads, sleep);
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

	private void printStatistics(int threads, int sleep) {
		_printStatistics(PRINT_LOYALTY_STATISTICS_URL, remoteConfig.getLoyaltyServiceHost(),
				remoteConfig.getLoyaltyServicePort(), threads, sleep);
		_printStatistics(PRINT_POST_STATISTICS_URL, remoteConfig.getPostServiceHost(),
				remoteConfig.getPostServicePort(), threads, sleep);
		_printStatistics(PRINT_EMAIL_STATISTICS_URL, remoteConfig.getEmailServiceHost(),
				remoteConfig.getEmailServicePort(), threads, sleep);
	}

	private void _printStatistics(String url, String host, String port, int threads, int sleep) {
		new AsyncRestTemplate().put(String.format(url, host, port, threads, sleep, null), null);
	}

	public void iterateCreateForAMinute(int repetitions, int interval, int threads, int start, int increment, int end) {
		for (int i = start; i <= end; i = i + increment) {
			createForAMinute(repetitions, interval, threads, i);
		}

	}

}
