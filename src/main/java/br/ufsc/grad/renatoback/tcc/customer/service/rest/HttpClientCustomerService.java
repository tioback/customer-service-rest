package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("http")
@Service
public class HttpClientCustomerService implements CustomerService {

	private Log logger = LogFactory.getLog(HttpClientCustomerService.class);

	private static final String LOYALTY_URL = "http://%s:%s/%d";
	private static final String POST_URL = "http://%s:%s/%d";
	private static final String EMAIL_URL = "http://%s:%s/%d";
	private static final String PRINT_LOYALTY_STATISTICS_URL = "http://%s:%s/%d/%d";
	private static final String PRINT_POST_STATISTICS_URL = "http://%s:%s/%d/%d";
	private static final String PRINT_EMAIL_STATISTICS_URL = "http://%s:%s/%d/%d";

	private AtomicInteger counter;
	private RemoteConfig rc;

	private AtomicCyclicCounter index;

	public HttpClientCustomerService(RemoteConfig rc) {
		this.rc = rc;
		this.counter = new AtomicInteger();
		this.index = new AtomicCyclicCounter(5);
	}

	private CloseableHttpClient httpclient;

	private void setUpConnections(int max) {
		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
		cm.setMaxTotal(max);

		httpclient = HttpClients.custom().setConnectionManager(cm).disableAutomaticRetries().build();
	}

	private void closeConnections() {
		try {
			httpclient.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void createCustomer() {
		try {
			setUpConnections(1);
			_createCustomer();
		} finally {
			closeConnections();
		}
	}

	private void _createCustomer() {
		_doProcessing();
		signalUserCreation(counter.incrementAndGet());
	}

	private void _doProcessing() {
		// TODO Adicionar algo que consuma tempo de processamento
	}

	private class SignalThread extends Thread {
		// private final CloseableHttpClient httpClient;
		private final HttpContext context;
		private final HttpUriRequest request;
		private final String name;

		public SignalThread(String name, HttpUriRequest request) {
			this.context = new BasicHttpContext();
			this.request = request;
			this.name = name;
		}

		/**
		 * Executes the GetMethod and prints some status information.
		 */
		@Override
		public void run() {
			try (CloseableHttpResponse response = httpclient.execute(request, context)) {
				// sent...
			} catch (Exception e) {
				sobra.incrementAndGet();
			}
		}
	}

	private void signalUserCreation(int userId) {

		long time = new Date().getTime();

		SignalThread[] threads = new SignalThread[3];

		switch (index.cyclicallyIncrementAndGet()) {
		case 0:
			threads[0] = doLoyalty(time);
			threads[1] = doPost(time);
			threads[2] = doEmail(time);
			break;
		case 1:
			threads[0] = doLoyalty(time);
			threads[1] = doEmail(time);
			threads[2] = doPost(time);
			break;
		case 2:
			threads[0] = doPost(time);
			threads[1] = doLoyalty(time);
			threads[2] = doEmail(time);
			break;
		case 3:
			threads[0] = doPost(time);
			threads[1] = doEmail(time);
			threads[2] = doLoyalty(time);
			break;
		case 4:
			threads[0] = doEmail(time);
			threads[1] = doPost(time);
			threads[2] = doLoyalty(time);
			break;
		case 5:
			threads[0] = doEmail(time);
			threads[1] = doLoyalty(time);
			threads[2] = doPost(time);
			break;
		}

		for (int j = 0; j < threads.length; j++) {
			threads[j].start();
		}

		for (int j = 0; j < threads.length; j++) {
			try {
				threads[j].join();
			} catch (InterruptedException e) {
				// e.printStackTrace();
			}
		}
	}

	private SignalThread doLoyalty(long time) {
		return post("Loyalty", LOYALTY_URL, rc.getLoyaltyServiceHost(), rc.getLoyaltyServicePort(), time);
	}

	private SignalThread doPost(long time) {
		return post("Post", POST_URL, rc.getPostServiceHost(), rc.getPostServicePort(), time);
	}

	private SignalThread doEmail(long time) {
		return post("Email", EMAIL_URL, rc.getEmailServiceHost(), rc.getEmailServicePort(), time);
	}

	private SignalThread post(String name, String baseUrl, String host, String port, long time) {
		return new SignalThread(name, new HttpPost(String.format(baseUrl, host, port, time)));
	}

	private AtomicInteger sobra;

	public void createForAMinute(int repetitions, int interval_seg, int threads, int sleep) {
		final long interval_millis = TimeUnit.SECONDS.toMillis(interval_seg);

		ExecutorService executor;
		for (int i = 0; i < repetitions; i++) {
			logger.info(String.format("[%d] - INICIO - [%d] Ativas", i, Thread.activeCount()));

			sobra = new AtomicInteger(0);

			clearStatistics();

			executor = setUpExecutor(interval_seg, threads);
			try {
				setUpConnections(threads);
				long start = System.currentTimeMillis();
				for (int j = 0; j < threads; j++) {
					executor.execute(() -> {
						interval_mk: while (System.currentTimeMillis() - start <= interval_millis) {
							_createCustomer();

							try {
								Thread.sleep(sleep);
							} catch (InterruptedException e) {
								break interval_mk;
							}
						}
					});
				}
				stopExecutor(interval_seg, executor);
			} finally {
				closeConnections();
			}

			printStatistics(threads, sleep);
			logger.info(String.format("[%d] - FIM - [%d] Canceladas", i, sobra.get()));
		}

	}

	private void stopExecutor(int interval_seg, ExecutorService executor) {
		executor.shutdown();
		try {
			if (!executor.awaitTermination(interval_seg, TimeUnit.SECONDS)) {
				sobra.addAndGet(executor.shutdownNow().size());
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private ThreadPoolExecutor setUpExecutor(int interval_seg, int threads) {
		return new ThreadPoolExecutor(0, Math.max(1, threads * 2 / 3), interval_seg, TimeUnit.SECONDS,
				new SynchronousQueue<>(), new RejectedExecutionHandler() {

					@Override
					public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
						sobra.incrementAndGet();
					}
				});
	}

	private void clearStatistics() {
		try {
			setUpConnections(3);

			SignalThread[] signals = new SignalThread[] {

					_clearStatistics("Loyalty", LOYALTY_URL, rc.getLoyaltyServiceHost(), rc.getLoyaltyServicePort()),
					_clearStatistics("Post", POST_URL, rc.getPostServiceHost(), rc.getPostServicePort()),
					_clearStatistics("Email", EMAIL_URL, rc.getEmailServiceHost(), rc.getEmailServicePort()) };

			for (int j = 0; j < signals.length; j++) {
				signals[j].start();
			}

			for (int j = 0; j < signals.length; j++) {
				try {
					signals[j].join();
				} catch (InterruptedException e) {
					// e.printStackTrace();
				}
			}
		} finally {
			closeConnections();
		}
	}

	private SignalThread _clearStatistics(String name, String baseUrl, String host, String port) {
		return delete(name, String.format(baseUrl, host, port, null));
	}

	private SignalThread delete(String name, String url) {
		return new SignalThread(name, new HttpDelete(url));
	}

	private void printStatistics(int threads, int sleep) {
		try {
			setUpConnections(3);

			SignalThread[] signals = new SignalThread[] {
					_printStatistics("Loyalty", PRINT_LOYALTY_STATISTICS_URL, rc.getLoyaltyServiceHost(),
							rc.getLoyaltyServicePort(), threads, sleep),
					_printStatistics("Post", PRINT_POST_STATISTICS_URL, rc.getPostServiceHost(),
							rc.getPostServicePort(), threads, sleep),
					_printStatistics("Email", PRINT_EMAIL_STATISTICS_URL, rc.getEmailServiceHost(),
							rc.getEmailServicePort(), threads, sleep) };

			for (int j = 0; j < signals.length; j++) {
				signals[j].start();
			}

			for (int j = 0; j < signals.length; j++) {
				try {
					signals[j].join();
				} catch (InterruptedException e) {
					// e.printStackTrace();
				}
			}
		} finally {
			closeConnections();
		}

	}

	private SignalThread _printStatistics(String name, String baseUrl, String host, String port, int threads,
			long sleep) {
		return put(name, String.format(baseUrl, host, port, threads, sleep, null));
	}

	private SignalThread put(String name, String url) {
		return new SignalThread(name, new HttpPut(url));
	}

	public void iterateCreateForAMinute(int repetitions, int interval, int threads, int start, int increment, int end) {
		for (int i = start; i <= end; i = i + increment) {
			createForAMinute(repetitions, interval, threads, i);
		}
	}

	// public void createForAMinute(int repetitions, int interval_seg, int
	// threads, int sleep) {
	// configAsyncHttpClient(threads, interval_seg);
	// _createForAMinute(repetitions, interval_seg, threads, sleep);
	// httpExecutorService.shutdownNow();
	// }
	//
	// public void _createForAMinute(int repetitions, int interval_seg, int
	// threads, int sleep) {
	// // BEGIN CONFIG
	// final long interval_millis = TimeUnit.SECONDS.toMillis(interval_seg);
	// // END CONFIG
	//
	// ExecutorService executor;
	// for (int i = 0; i < repetitions; i++) {
	// logger.info(String.format("Starting batch %d", i));
	// int activeThreadsCount = Thread.activeCount();
	// System.err.println(String.format("Starting batch %d [%d]", i,
	// activeThreadsCount));
	// clearStatistics();
	//
	// executor = Executors.newFixedThreadPool(threads);
	// long start = System.nanoTime();
	// for (int j = 0; j < threads; j++) {
	// executor.execute(() -> {
	// interval_mk: while (new Date().getTime() - start < interval_millis) {
	// createCustomer();
	//
	// try {
	// Thread.sleep(sleep);
	// } catch (InterruptedException e) {
	// break interval_mk;
	// }
	// }
	// });
	// }
	// executor.shutdown();
	// int sobra = 0;
	// try {
	// if (!executor.awaitTermination(interval_seg, TimeUnit.SECONDS)) {
	// sobra = executor.shutdownNow().size();
	// }
	// } catch (InterruptedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// logger.info(String.format("Fim do processamento com %d threads não
	// processadas.", sobra));
	// printStatistics(threads, sleep);
	// }
	// }
	//
	// private void clearStatistics() {
	// _clearStatistics("Loyalty", LOYALTY_URL, rc.getLoyaltyServiceHost(),
	// rc.getLoyaltyServicePort());
	// _clearStatistics("Post", POST_URL, rc.getPostServiceHost(),
	// rc.getPostServicePort());
	// _clearStatistics("Email", EMAIL_URL, rc.getEmailServiceHost(),
	// rc.getEmailServicePort());
	// }
	//
	// private void _clearStatistics(String name, String baseUrl, String host,
	// String port) {
	// final int total = 3;
	// int retry = 0;
	// while (retry++ < total) {
	// try {
	// delete(String.format(baseUrl, host, port, null));
	// return;
	// } catch (Exception e) {
	// System.err.println(
	// String.format("Error clearing statistics for %s, retrying... [%d/%d]",
	// name, retry, total));
	// try {
	// Thread.sleep(100);
	// } catch (InterruptedException e1) {
	// }
	// }
	// }
	// System.err.println("Quitting after " + total + " errors trying to clear
	// statistics for " + name);
	// }
	//
	// private void printStatistics(int threads, int sleep) {
	// _printStatistics("Loyalty", PRINT_LOYALTY_STATISTICS_URL,
	// rc.getLoyaltyServiceHost(),
	// rc.getLoyaltyServicePort(), threads, sleep);
	// _printStatistics("Post", PRINT_POST_STATISTICS_URL,
	// rc.getPostServiceHost(), rc.getPostServicePort(), threads,
	// sleep);
	// _printStatistics("Email", PRINT_EMAIL_STATISTICS_URL,
	// rc.getEmailServiceHost(), rc.getEmailServicePort(),
	// threads, sleep);
	// }
	//
	// private void _printStatistics(String name, String url, String host,
	// String port, int threads, int sleep) {
	// final int total = 3;
	// int retry = 0;
	// while (retry++ < total) {
	// try {
	// put(String.format(url, host, port, threads, sleep, null));
	// return;
	// } catch (Exception e) {
	// System.err.println(
	// String.format("Error printing statistics for %s, retrying... [%d/%d]",
	// name, retry, total));
	// try {
	// Thread.sleep(100);
	// } catch (InterruptedException e1) {
	// }
	// }
	// }
	// System.err.println("Quitting after " + total + " errors trying to print
	// statistics for " + name);
	// }
	//
	// public void iterateCreateForAMinute(int repetitions, int interval, int
	// threads, int start, int increment, int end) {
	// configAsyncHttpClient(threads, interval);
	// for (int i = start; i <= end; i = i + increment) {
	// _createForAMinute(repetitions, interval, threads, i);
	// }
	// httpExecutorService.shutdownNow();
	// }

}
