package com.mobistock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MobiStockApplication {

	public static void main(String[] args) {
		SpringApplication.run(MobiStockApplication.class, args);
	}

	@jakarta.annotation.PostConstruct
	public void init() {
		java.util.Locale.setDefault(new java.util.Locale("en", "PK"));
	}

	@org.springframework.context.annotation.Bean
	public org.springframework.web.servlet.LocaleResolver localeResolver() {
		org.springframework.web.servlet.i18n.SessionLocaleResolver slr = new org.springframework.web.servlet.i18n.SessionLocaleResolver();
		slr.setDefaultLocale(new java.util.Locale("en", "PK"));
		return slr;
	}

}
