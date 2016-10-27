package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class RemoteConfigWrapper {

	@Bean
	public RemoteConfig remoteConfig() {
		return new RemoteConfig();
	}

	public static class RemoteConfig {

		@Autowired
		Environment env;

		public String getLoyaltyServiceHost() {
			return env.getProperty("loyalty.service.host", "localhost");
		}

		public String getPostServiceHost() {
			return env.getProperty("post.service.host", "localhost");
		}

		public String getEmailServiceHost() {
			return env.getProperty("email.service.host", "localhost");
		}

	}

}
