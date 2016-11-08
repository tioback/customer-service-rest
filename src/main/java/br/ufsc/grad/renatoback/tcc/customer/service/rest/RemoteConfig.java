package br.ufsc.grad.renatoback.tcc.customer.service.rest;

public interface RemoteConfig {
	String getLoyaltyServiceHost();

	String getPostServiceHost();

	String getEmailServiceHost();

	String getLoyaltyServicePort();

	String getPostServicePort();

	String getEmailServicePort();
}
