package com.travelplanner.TravelPlanner;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import com.travelplanner.TravelPlanner.service.BotService;

@SpringBootApplication
public class TravelPlannerApplication {

	@Autowired
	private BotService botService;

	public static void main(String[] args) {
		SpringApplication.run(TravelPlannerApplication.class, args);
	}

	@PostConstruct
	public void startTelegramBot() {
		try {
			TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
			botsApi.registerBot(botService);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}