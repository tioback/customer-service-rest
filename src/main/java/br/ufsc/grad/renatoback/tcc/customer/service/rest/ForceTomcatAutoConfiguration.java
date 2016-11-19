package br.ufsc.grad.renatoback.tcc.customer.service.rest;

import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;

//
//@Configuration
//@AutoConfigureBefore(EmbeddedServletContainerAutoConfiguration.class)
public class ForceTomcatAutoConfiguration {

	@Bean
	TomcatEmbeddedServletContainerFactory tomcat() {
		return new TomcatEmbeddedServletContainerFactory();
	}
}
