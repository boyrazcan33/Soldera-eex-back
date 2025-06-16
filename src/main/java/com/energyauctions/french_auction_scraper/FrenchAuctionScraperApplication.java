package com.energyauctions.french_auction_scraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class FrenchAuctionScraperApplication {

	public static void main(String[] args) {
		SpringApplication.run(FrenchAuctionScraperApplication.class, args);
	}

}
