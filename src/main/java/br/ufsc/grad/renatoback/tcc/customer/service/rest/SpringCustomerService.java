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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

@Profile("spring")
@Service
public class SpringCustomerService implements CustomerService {

	private Log logger = LogFactory.getLog(SpringCustomerService.class);

	private static final String LOYALTY_URL = "http://%s:%s/%d";
	private static final String POST_URL = "http://%s:%s/%d";
	private static final String EMAIL_URL = "http://%s:%s/%d";
	private static final boolean FOLLOW_REDIRECTS = true;
	private static final boolean CONNECTION_POOLING = true;

	private AtomicInteger counter;
	private RemoteConfig rc;
	private AtomicCyclicCounter index;
	private AsyncHttpClient asyncHttpClient;
	private ExecutorService httpExecutorService;

	public SpringCustomerService(RemoteConfig rc) {
		this.rc = rc;
		counter = new AtomicInteger();
		index = new AtomicCyclicCounter(5);
	}

	public void createCustomer() {
		configAsyncHttpClient(15, 5);
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
		final long interval_nano = TimeUnit.SECONDS.toNanos(interval_seg);
		// END CONFIG

		ExecutorService executor;
		for (int i = 0; i < repetitions; i++) {
			int activeThreadsCount = Thread.activeCount();
			executor = Executors.newFixedThreadPool(threads);
			long start = System.nanoTime();

			for (int j = 0; j < threads; j++) {
				executor.execute(new Task(this, start, interval_nano, sleep));
			}
			int sobra = stopExecutor(interval_seg, executor);

			logger.info(String.format("FIM [Iteração][Ativas][Interrompidas] - [%d][%d][%d]", i, activeThreadsCount,
					sobra));

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
			e.printStackTrace();
		}
		return sobra;
	}

	public void printStatistics(int threads, int sleep) {
	}

	public void iterateCreateForAMinute(int repetitions, int interval, int threads, int start, int increment, int end) {
		configAsyncHttpClient(threads, interval);
		for (int i = start; i <= end; i = i + increment) {
			_createForAMinute(repetitions, interval, threads, i);
		}
		httpExecutorService.shutdownNow();
	}

}