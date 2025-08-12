# AI Travel Planner Telegram Bot

![Telegram Bot](https://img.shields.io/badge/Telegram-Bot-blue?logo=telegram)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-v2.7-green)
![Java](https://img.shields.io/badge/Java-17-orange)
![Redis](https://img.shields.io/badge/Redis-In_Memory-red)

---

## Overview

AI Travel Planner is a Spring Boot based Telegram bot that interacts with users to create personalized travel itineraries. Using Telegram's messaging and inline buttons, it collects trip preferences, fetches weather updates, and generates customized trip plans leveraging a local AI model (Mistral). The final trip plan is compiled into a PDF and sent back to the user.

---

## Key Features

- **User Interaction:** Collects user inputs via Telegram messages and inline buttons with robust validation.
- **State Management:** Uses Redis as an in-memory data store to track user conversation states.
- **Data Persistence:** Stores trip plans and user data using H2 in-memory database for development.
- **Weather Integration:** Retrieves weather forecasts for the trip dates and destinations.
- **AI Integration:** Generates detailed trip plans with a locally hosted Mistral AI language model.
- **PDF Generation:** Creates and sends downloadable PDF itineraries to users.
- **Error Handling:** Validates inputs and resets conversations on invalid data.

---

## Architecture & Technologies

| Technology     | Purpose                                   |
|----------------|-------------------------------------------|
| Java 17        | Programming Language                      |
| Spring Boot    | Application Framework                     |
| Telegram API   | User Interaction                         |
| Redis          | In-memory state management                |
| H2 Database    | Temporary data persistence during runtime |
| Mistral AI     | Local AI model for trip plan generation  |
| iText 7        | PDF generation library                    |

---

## Getting Started

### Prerequisites

- Java 17 or above installed
- Redis server running locally (default port 6379)
- Telegram Bot Token (create your bot via [BotFather](https://t.me/BotFather))
- Local Mistral AI model API running (at `http://localhost:11434/api`)

### Setup Instructions

1. **Clone the repo**

```bash
git clone https://github.com/yourusername/Ai-Travel-Planner.git
cd Ai-Travel-Planner
Configure application properties

Rename src/main/resources/application.properties.example to application.properties:

bash
Copy
Edit
mv src/main/resources/application.properties.example src/main/resources/application.properties
Edit application.properties and set your Telegram bot token and username:

properties
Copy
Edit
telegram.bot.token=YOUR_TELEGRAM_BOT_TOKEN_HERE
telegram.bot.username=YOUR_BOT_USERNAME_HERE
Start Redis server

Ensure Redis is installed and running locally on port 6379.

Build and run the application

bash
Copy
Edit
./mvnw clean install
./mvnw spring-boot:run
Interact with your bot

Search your bot on Telegram using its username and start chatting!

Usage Flow
User starts conversation → bot sets state to start.

Bot collects trip destination, dates, interests via messages and buttons.

Bot validates inputs and fetches weather updates.

Bot generates trip plan using Mistral AI.

Bot creates a PDF itinerary and sends it to the user.

Bot resets or continues session based on user inputs.

Important Notes
Security: Never commit your real Telegram bot token or sensitive info to GitHub.

In-Memory DB: H2 is used only for temporary storage during runtime.

Redis: Used for managing user conversation states to provide a seamless experience.

AI Model: Local Mistral instance required to generate trip plans.

PDFs: Generated PDFs are stored temporarily before sending.

Demo Video
https://www.linkedin.com/posts/mohammed-shakir-732b50307_backenddevelopment-springboot-java-activity-7361045600450093056-gNkD

Contributing
Feel free to fork the repo and submit pull requests or raise issues!

GitHub: shakir-18