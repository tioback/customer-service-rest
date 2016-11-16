package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("dev")
@Configuration
public class LocalConfigWrapper {

	@Bean
	public RemoteConfig remoteConfig() {
		return new RemoteConfig() {
			public String getLoyaltyServiceHost() {
				return "localhost";
			}

			public String getPostServiceHost() {
				return "localhost";
			}

			public String getEmailServiceHost() {
				return "localhost";
			}

			public String getLoyaltyServicePort() {
				return "8235";
			}

			public String getPostServicePort() {
				return "8236";
			}

			public String getEmailServicePort() {
				return "8237";
			}
		};
	}

}
