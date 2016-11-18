package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

@Service
public class SpringCustomerService implements CustomerService {

	private Log logger = LogFactory.getLog(SpringCustomerService.class);

	private static final String LOYALTY_URL = "http://%s:%s/%d";
	private static final String POST_URL = "http://%s:%s/%d";
	private static final String EMAIL_URL = "http://%s:%s/%d";
	private static final String PRINT_LOYALTY_STATISTICS_URL = "http://%s:%s/%d/%d";
	private static final String PRINT_POST_STATISTICS_URL = "http://%s:%s/%d/%d";
	private static final String PRINT_EMAIL_STATISTICS_URL = "http://%s:%s/%d/%d";

	AtomicInteger counter = new AtomicInteger();

	RemoteConfig rc;

	public SpringCustomerService(RemoteConfig rc) {
		this.rc = rc;
	}

	public void createCustomer() {
		_doProcessing();

		signalUserCreation(counter.incrementAndGet());
	}

	private void _doProcessing() {
		// TODO Adicionar algo que consuma tempo de processamento
	}

	AtomicCyclicCounter index = new AtomicCyclicCounter(5);

	private void signalUserCreation(int userId) {

		long time = new Date().getTime();

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

	private AsyncHttpClient asyncHttpClient;

	boolean followRedirects = true;
	boolean connectionPooling = true;
	ExecutorService httpExecutorService;

	private void configAsyncHttpClient(int threads, int interval) {
		System.err.println("Configurando novo client...");
		httpExecutorService = new ThreadPoolExecutor(0, threads * 2, interval, TimeUnit.SECONDS,
				new SynchronousQueue<>());
		// httpExecutorService = Executors.newFixedThreadPool(threads);
		AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setExecutorService(httpExecutorService)
				.setMaxConnectionsPerHost(1000).setAllowPoolingConnections(connectionPooling)
				.setAllowPoolingSslConnections(connectionPooling).setConnectTimeout((int) TimeUnit.SECONDS.toMillis(1))
				.setRequestTimeout((int) TimeUnit.SECONDS.toMillis(1)).setCompressionEnforced(true)
				.setFollowRedirect(followRedirects).build();
		asyncHttpClient = new AsyncHttpClient(config);
	}

	private void delete(String url) {
		new RestTemplate().delete(url);

		// try {
		// asyncHttpClient.prepareDelete(url).execute().get(1,
		// TimeUnit.SECONDS);
		// } catch (InterruptedException | ExecutionException | TimeoutException
		// e) {
		// // TODO Auto-generated catch block
		// // e.printStackTrace();
		// }
		return;
	}

	private void put(String url) {
		new RestTemplate().put(url, null);

		// try {
		// asyncHttpClient.preparePut(url).execute().get(1, TimeUnit.SECONDS);
		// } catch (InterruptedException | ExecutionException | TimeoutException
		// e) {
		// // TODO Auto-generated catch block
		// // e.printStackTrace();
		// }
		return;
	}

	private void post(String url) {
		// new AsyncRestTemplate().postForLocation(url, null);

		asyncHttpClient.preparePost(url).execute();
		return;
	}

	private void doLoyalty(long time) {
		post(String.format(LOYALTY_URL, rc.getLoyaltyServiceHost(), rc.getLoyaltyServicePort(), time));
		return;
	}

	private void doPost(long time) {
		post(String.format(POST_URL, rc.getPostServiceHost(), rc.getPostServicePort(), time));
		return;
	}

	private void doEmail(long time) {
		post(String.format(EMAIL_URL, rc.getEmailServiceHost(), rc.getEmailServicePort(), time));
		return;
	}

	public void createForAMinute(int repetitions, int interval_seg, int threads, int sleep) {
		configAsyncHttpClient(threads, interval_seg);
		_createForAMinute(repetitions, interval_seg, threads, sleep);
		httpExecutorService.shutdownNow();
	}

	public void _createForAMinute(int repetitions, int interval_seg, int threads, int sleep) {
		// BEGIN CONFIG
		final long interval_millis = TimeUnit.SECONDS.toMillis(interval_seg);
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
					interval_mk: while (new Date().getTime() - start < interval_millis) {
						createCustomer();

						try {
							Thread.sleep(sleep);
						} catch (InterruptedException e) {
							break interval_mk;
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
		_clearStatistics(LOYALTY_URL, rc.getLoyaltyServiceHost(), rc.getLoyaltyServicePort());
		_clearStatistics(POST_URL, rc.getPostServiceHost(), rc.getPostServicePort());
		_clearStatistics(EMAIL_URL, rc.getEmailServiceHost(), rc.getEmailServicePort());
	}

	private void _clearStatistics(String baseUrl, String host, String port) {
		delete(String.format(baseUrl, host, port, null));
	}

	private void printStatistics(int threads, int sleep) {
		_printStatistics(PRINT_LOYALTY_STATISTICS_URL, rc.getLoyaltyServiceHost(), rc.getLoyaltyServicePort(), threads,
				sleep);
		_printStatistics(PRINT_POST_STATISTICS_URL, rc.getPostServiceHost(), rc.getPostServicePort(), threads, sleep);
		_printStatistics(PRINT_EMAIL_STATISTICS_URL, rc.getEmailServiceHost(), rc.getEmailServicePort(), threads,
				sleep);
	}

	private void _printStatistics(String url, String host, String port, int threads, int sleep) {
		put(String.format(url, host, port, threads, sleep, null));
	}

	public void iterateCreateForAMinute(int repetitions, int interval, int threads, int start, int increment, int end) {
		configAsyncHttpClient(threads, interval);
		for (int i = start; i <= end; i = i + increment) {
			_createForAMinute(repetitions, interval, threads, i);
		}
		httpExecutorService.shutdownNow();
	}
}