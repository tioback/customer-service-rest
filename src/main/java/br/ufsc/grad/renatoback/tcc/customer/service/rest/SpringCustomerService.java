package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

@Profile("spring")
@Service
public class SpringCustomerService implements CustomerService {

	private Log logger = LogFactory.getLog(SpringCustomerService.class);

	private static final String LOYALTY_URL = "http://%s:%s/%d";
	private static final String POST_URL = "http://%s:%s/%d";
	private static final String EMAIL_URL = "http://%s:%s/%d";
	private static final String LOYALTY_SYNC_URL = "http://%s:%s/sync/%d";
	private static final String POST_SYNC_URL = "http://%s:%s/sync/%d";
	private static final String EMAIL_SYNC_URL = "http://%s:%s/sync/%d";
	private static final String PRINT_LOYALTY_STATISTICS_URL = "http://%s:%s/%d/%d";
	private static final String PRINT_POST_STATISTICS_URL = "http://%s:%s/%d/%d";
	private static final String PRINT_EMAIL_STATISTICS_URL = "http://%s:%s/%d/%d";
	private static final boolean FOLLOW_REDIRECTS = true;
	private static final boolean CONNECTION_POOLING = true;
	private static final Long BASE_TIME = 379089900000l;

	private AtomicBoolean synchronizedTime;
	private AtomicInteger counter;
	private RemoteConfig rc;
	private RestTemplate restTemplate;
	private AtomicCyclicCounter index;
	private AsyncHttpClient asyncHttpClient;
	private ExecutorService httpExecutorService;

	public SpringCustomerService(RemoteConfig rc) {
		this.rc = rc;
		synchronizedTime = new AtomicBoolean(false);
		counter = new AtomicInteger();
		index = new AtomicCyclicCounter(5);
	}

	public void createCustomer() {
		configAsyncHttpClient(15, 5);
		synchronizeTime();
		create_Customer();
		httpExecutorService.shutdownNow();
	}

	public void create_Customer() {
		_doProcessing();

		signalUserCreation(counter.incrementAndGet());
	}

	private void _doProcessing() {
		// TODO Adicionar algo que consuma tempo de processamento
	}

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

	private void configAsyncHttpClient(int threads, int interval) {
		System.err.println("Configurando novo client...");
		httpExecutorService = new ThreadPoolExecutor(0, threads * 2, interval, TimeUnit.SECONDS,
				new SynchronousQueue<>());
		// httpExecutorService = Executors.newFixedThreadPool(threads);
		AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setExecutorService(httpExecutorService)
				.setMaxConnectionsPerHost(1000).setAllowPoolingConnections(CONNECTION_POOLING)
				.setAllowPoolingSslConnections(CONNECTION_POOLING).setConnectTimeout((int) TimeUnit.SECONDS.toMillis(1))
				.setRequestTimeout((int) TimeUnit.SECONDS.toMillis(1)).setCompressionEnforced(true)
				.setFollowRedirect(FOLLOW_REDIRECTS).build();
		asyncHttpClient = new AsyncHttpClient(config);

		restTemplate = new RestTemplate();
	}

	private void delete(String url) {
		restTemplate.delete(url);

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
		restTemplate.put(url, null);

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
		synchronizeTime();
		_createForAMinute(repetitions, interval_seg, threads, sleep);
		httpExecutorService.shutdownNow();
	}

	public void _createForAMinute(int repetitions, int interval_seg, int threads, int sleep) {
		// BEGIN CONFIG
		final long interval_millis = TimeUnit.SECONDS.toMillis(interval_seg);
		// END CONFIG

		ExecutorService executor;
		for (int i = 0; i < repetitions; i++) {
			int activeThreadsCount = Thread.activeCount();
			logger.info(String.format("[%d] - INICIO - [%d] ativas", i, activeThreadsCount));

			clearStatistics();

			executor = Executors.newFixedThreadPool(threads);
			long start = System.currentTimeMillis();

			for (int j = 0; j < threads; j++) {
				executor.execute(new Task(this, start, interval_millis, sleep));
			}
			int sobra = stopExecutor(interval_seg, executor);

			logger.info(String.format("[%d] - FIM    - [%d] interrompidas", i, sobra));

			printStatistics(threads, sleep);
		}
	}

	private int stopExecutor(int interval_seg, ExecutorService executor) {
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
		return sobra;
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
		synchronizeTime();
		for (int i = start; i <= end; i = i + increment) {
			_createForAMinute(repetitions, interval, threads, i);
		}
		httpExecutorService.shutdownNow();
	}

	private void synchronizeTime() {
		if (synchronizedTime.get()) {
			return;
		}

		long diff = System.currentTimeMillis() - BASE_TIME;
		_syncClock(LOYALTY_SYNC_URL, rc.getLoyaltyServiceHost(), rc.getLoyaltyServicePort(), diff);
		_syncClock(POST_SYNC_URL, rc.getPostServiceHost(), rc.getPostServicePort(), diff);
		_syncClock(EMAIL_SYNC_URL, rc.getEmailServiceHost(), rc.getEmailServicePort(), diff);
		synchronizedTime.set(true);
	}

	private void _syncClock(String url, String host, String port, long diff) {
		put(String.format(url, host, port, diff));
	}

}