package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("heroku")
@Configuration
public class HerokuConfigWrapper {

	@Bean
	public RemoteConfig remoteConfig() {
		return new RemoteConfig() {
			public String getLoyaltyServiceHost() {
				return "loyalty-service-rest.herokuapp.com";
			}

			public String getPostServiceHost() {
				return "post-service-rest.herokuapp.com";
			}

			public String getEmailServiceHost() {
				return "email-service-rest.herokuapp.com";
			}

			public String getLoyaltyServicePort() {
				return "80";
			}

			public String getPostServicePort() {
				return "80";
			}

			public String getEmailServicePort() {
				return "80";
			}
		};
	}
}
