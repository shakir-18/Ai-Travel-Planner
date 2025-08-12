package com.travelplanner.TravelPlanner.service;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.travelplanner.TravelPlanner.entity.Trip;
import com.travelplanner.TravelPlanner.entity.TripPlan;
import com.travelplanner.TravelPlanner.entity.Users;
import com.travelplanner.TravelPlanner.repo.TripPlanRepository;
import com.travelplanner.TravelPlanner.repo.TripRepository;
import com.travelplanner.TravelPlanner.repo.UserRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Location;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;


@Service
public class BotService extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TripRepository tripRepository;
    @Autowired
    private StateService service;
    @Autowired
    private GeneratePrompt generatePrompt;
    @Autowired
    private OllamaService ollamaService;
    @Autowired
    private TripPlanRepository tripPlanRepository;
    @Value("${telegram.bot.token}")
    private String token;
    @Value("${telegram.bot.username}")
    private String username;
    private Map<String, String> placeCallbackMap = new ConcurrentHashMap<>();
    Trip trip=new Trip();
    @Override
    public String getBotUsername() {
        return username;
    }
    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update)
    {
        if(update.hasMessage())
        {
            Long id=update.getMessage().getChatId();
            String text=update.getMessage().getText();
            SendMessage message=new SendMessage();
            message.setChatId(id.toString());
            String state=service.getUserState(id);
            if (state == null || state.isEmpty()) {
                service.setUserState(id, "start");
                state = "start";
            }
            if (update.getMessage().hasLocation() && "askLocation".equals(state)) {
                Location loc = update.getMessage().getLocation();
                trip.setLatitude(loc.getLatitude());
                trip.setLongitude(loc.getLongitude());
                service.setUserState(id, "askDuration");
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(id.toString());
                sendMessage.setText("Select the trip duration period:");
                askDuration(sendMessage, id);
                return;
            }
            if(update.getMessage().hasText()) {
                switch (state) {
                    case "start" -> begin(message, id);
                    case "place" -> getPlace(message, id, text);
                    case "trip", "gotPlace", "askDuration", "askLocation" -> {
                        service.setUserState(id, "start");
                        message.setText("Invalid message, please start again.");
                        try {
                            execute(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    case "askDate" -> {
                        if (validateDate(text)) {
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                            sdf.setLenient(false);
                            try {
                                Date date = sdf.parse(text);
                                trip.setStartDate(date);
                                JSONObject weatherData = weatherUpdates(trip.getLatitude(), trip.getLongitude());
                                String startDateStr = sdf.format(trip.getStartDate());
                                try {
                                    String x = getForecastFromDate(weatherData, startDateStr).toString();
                                    trip.setWeatherUpdatesJson(x);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    service.setUserState(id, "start");
                                    message.setText("Failed to get weather data. Please start again.");
                                    try {
                                        execute(message);
                                    } catch (Exception p) {
                                        p.printStackTrace();
                                    }
                                    break;
                                }
                                //calling interests function!
                                interests(message, id);
                            } catch (ParseException e) {
                                e.printStackTrace();
                                service.setUserState(id, "start");
                                message.setText("Invalid date format! Please enter date as YYYY-MM-DD.");
                                try {
                                    execute(message);
                                } catch (Exception p) {
                                    p.printStackTrace();
                                }
                            }
                        } else {
                            service.setUserState(id, "start");
                            message.setText("Invalid date format! Please start again.");
                            try {
                                execute(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    case "interests" -> {
                        try {
                            sendMessage(id, "Your data has been collected and the trip plan is being generated. Please wait...");

                            String weatherMessage = formatWeatherMessage(trip.getWeatherUpdatesJson());
                            sendMessage(id, weatherMessage);

                            String tips = getHealthyTravelTips();
                            sendMessage(id, tips);

                            StringBuilder pdfContent = new StringBuilder();
                            pdfContent.append("Weather Updates:\n").append(weatherMessage).append("\n\n");
                            pdfContent.append("Healthy Travel Tips:\n").append(tips).append("\n\n");
                            String tripPlanResponse = ollamaService.generateFullTripPlan(generatePrompt.getPrompt(trip, id));
                            JSONObject json = new JSONObject(tripPlanResponse);
                            String generatedText = json.getString("response");  // or the key that contains the AI text
                            pdfContent.append("AI Trip Plan:\n").append(generatedText);
                            TripPlan tripPlan = new TripPlan();
                            tripPlan.setUserId(id);
                            tripPlan.setDestination(trip.getDestination());
                            tripPlan.setPlanText(generatedText);
                            tripPlan.setGeneratedAt(LocalDateTime.now());
                            tripPlanRepository.save(tripPlan);
                            String tempDir = System.getProperty("java.io.tmpdir");
                            String filePath = tempDir + File.separator + "trip_plan_" + id + ".pdf";
                            try {
                                createTripPdf(filePath, pdfContent.toString());
                                sendPdfDocument(id, filePath);
                                completed(new SendMessage(id.toString(), "Your trip is here! Download it. Have a " +
                                        "Great Journey"), id);
                            } catch (Exception e) {
                                sendMessage(id, "Pdf generation failed! Please try again from start.");
                                e.printStackTrace();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            service.setUserState(id, "start");
                            message.setText("Failed to generate trip. Please start again.");
                            try {
                                execute(message);
                            } catch (Exception p) {
                                p.printStackTrace();
                            }
                        }
                    }
                    default -> {
                        break;
                    }
                }
            }
        }
        else if (update.hasCallbackQuery()) {
            Long id=update.getCallbackQuery().getMessage().getChatId();
            String text=update.getCallbackQuery().getData();
            SendMessage message=new SendMessage();
            message.setChatId(id.toString());
            String state=service.getUserState(id);
            if (state == null || state.isEmpty()) {
                service.setUserState(id, "start");
                state = "start";
            }
            switch (state) {
                case "trip" -> {
                    if (text.equals("new")) {
                        newTrip(message, id);
                    } else {
                        oldTrips(message, id);
                    }
                }
                case "gotPlace" -> {
                    if (text.startsWith("place")) {
                        String place = placeCallbackMap.get(text);
                        if (place != null) {
                            Users user = userRepository.findById(id).orElse(null);
                            if (user != null) {
                                trip.setUsers(user);
                                trip.setDestination(place);
                                askLocation(message, id, "Your Location Please! ");
                            }
                        }
                    } else {
                        message.setText("Invalid place selection, please try again.");
                        try {
                            execute(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                case "askDuration" -> {
                    trip.setDuration(text);
                    tripRepository.save(trip);
                    askDate(message, id);
                }
                default -> {
                    break;
                }
            }
        }
    }
    public void begin(SendMessage message,Long id)
    {
        if (!userRepository.existsById(id)) {
            userRepository.save(new Users(id));
            message.setText("WELCOME! THIS IS AN AI TRAVEL PLANNER.");
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("New Trip");
            button.setCallbackData("new");
            inlineKeyboardMarkup.setKeyboard(List.of(List.of(button)));
            message.setReplyMarkup(inlineKeyboardMarkup);
            try {
                execute(message);
                service.setUserState(id,"trip");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            message.setText("WELCOME BACK TO THE AI TRAVEL PLANNER.");
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<InlineKeyboardButton> buttons = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText("New Trip");
            button.setCallbackData("new");
            buttons.add(button);
            InlineKeyboardButton button1=new InlineKeyboardButton();
            button1.setText("Past Trips");
            button1.setCallbackData("old");
            buttons.add(button1);
            inlineKeyboardMarkup.setKeyboard(List.of(buttons));
            message.setReplyMarkup(inlineKeyboardMarkup);
            try {
                execute(message);
                service.setUserState(id,"trip");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void newTrip(SendMessage message,Long id)
    {
        message.setText("Enter the place name : ");
        try {
            execute(message);
            service.setUserState(id,"place");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void oldTrips(SendMessage message,Long id)
    {
        List<TripPlan> userTrips=tripPlanRepository.findByUserId(id);
        message.setText("Latest Trip : "+ userTrips.get(0).getPlanText());
        try {
            execute(message);
            service.setUserState(id,"start");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void getPlace(SendMessage message,Long id, String place)
    {
        List<String> result=new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(place, StandardCharsets.UTF_8);
            String urlStr = "https://nominatim.openstreetmap.org/search?format=json&q=" + encodedQuery;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty(
                    "User-Agent",
                    "shakir_shakir_2025/1.0 (contact: shakir426@example.com)"
            );

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONArray jsonArr = new JSONArray(response.toString());
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject obj = jsonArr.getJSONObject(i);
                JSONObject address = obj.optJSONObject("address");
                String placeName = "";
                if (address != null) {
                    if (address.has("city")) {
                        placeName = address.getString("city");
                    } else if (address.has("town")) {
                        placeName = address.getString("town");
                    } else if (address.has("village")) {
                        placeName = address.getString("village");
                    } else if (address.has("state")) {
                        placeName = address.getString("state");
                    } else if (address.has("country")) {
                        placeName = address.getString("country");
                    }
                }
                if (placeName.isEmpty()) {
                    placeName = obj.getString("display_name");
                }
                result.add(placeName);
                String lat = obj.getString("lat");
                String lon = obj.getString("lon");
            }
            InlineKeyboardMarkup markup=new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> list=new ArrayList<>();
            for (int i = 0; i < result.size(); i++) {
                String placeName = result.get(i);
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(placeName);
                String callbackData = "place_" + i;
                button.setCallbackData(callbackData);
                placeCallbackMap.put(callbackData, placeName);
                list.add(List.of(button));
            }
            markup.setKeyboard(list);
            message.setText("Select your place from the below suggestions : ");
            message.setReplyMarkup(markup);
            try {
                execute(message);
                service.setUserState(id,"gotPlace");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void askLocation(SendMessage message,Long id,String text)
    {
        KeyboardButton button=new KeyboardButton();
        button.setText("\uD83D\uDCCD Share Location");
        button.setRequestLocation(true);
        KeyboardRow row = new KeyboardRow();
        row.add(button);
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setKeyboard(List.of(row));
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);
        message.setReplyMarkup(keyboardMarkup);
        message.setText(text);
        try {
            execute(message);
            service.setUserState(id, "askLocation");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void askDuration(SendMessage message,Long id)
    {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText("1 day");
        button.setCallbackData("1 days");
        InlineKeyboardButton button1 = new InlineKeyboardButton();
        button1.setText("2-3 days");
        button1.setCallbackData("2-3 days");
        InlineKeyboardButton button2 = new InlineKeyboardButton();
        button2.setText("4-7 days");
        button2.setCallbackData("4-7 days");
        List<InlineKeyboardButton> row = List.of(button, button1, button2);
        inlineKeyboardMarkup.setKeyboard(List.of(row));
        message.setReplyMarkup(inlineKeyboardMarkup);
        try {
            execute(message);
            service.setUserState(id,"askDuration");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void completed(SendMessage message,Long id)
    {
        try {
            execute(message);
            service.setUserState(id,"start");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void askDate(SendMessage message,Long id)
    {
        message.setText("Select the starting date in YYYY-MM-DD Format only and the entered Date should not be" +
                " too long from now : ");
        try {
            execute(message);
            service.setUserState(id,"askDate");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean validateDate(String text)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);

        try {
            Date date = sdf.parse(text);
            Date today = new Date();
            if (date.before(today)) {
                return false;
            }

            long diffInMillis = date.getTime() - today.getTime();
            long diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis);

            return diffInDays <= 20;
        } catch (ParseException e) {
            return false;
        }
    }
    public JSONObject weatherUpdates(double lat, double lon)
    {
        try {
            String apiKey = "b3a696173175f4ba0e9935f616b59fa8";
            String urlStr = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&daily=temperature_2m_max,temperature_2m_min&timezone=auto",
                    lat, lon);

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            int responseCode=conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                return new JSONObject(response.toString());
            } else if (responseCode == 401) {
                System.err.println("Unauthorized: Check your API key.");
                return null;
            } else {
                System.err.println("Request failed. Response Code: " + responseCode);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static List<JSONObject> getForecastFromDate(JSONObject weatherData, String fromDate) throws Exception {
        if (weatherData == null) {
            throw new IllegalArgumentException("weatherData is null. Cannot get forecast.");
        }
        List<JSONObject> filteredForecasts = new ArrayList<>();
        JSONObject daily = weatherData.getJSONObject("daily");
        JSONArray times = daily.getJSONArray("time");
        JSONArray maxTemps = daily.getJSONArray("temperature_2m_max");
        JSONArray minTemps = daily.getJSONArray("temperature_2m_min");
        LocalDate from = LocalDate.parse(fromDate);

        for (int i = 0; i < times.length(); i++) {
            LocalDate currentDate = LocalDate.parse(times.getString(i));
            if (!currentDate.isBefore(from)) {
                JSONObject dayForecast = new JSONObject();
                dayForecast.put("date", currentDate.toString());
                dayForecast.put("temp_max", maxTemps.getDouble(i));
                dayForecast.put("temp_min", minTemps.getDouble(i));
                filteredForecasts.add(dayForecast);
            }
        }
        return filteredForecasts;
    }
    public void interests(SendMessage message,Long id)
    {
        message.setText("Please type your interests in this format only. \n History, Nature, Beaches, Island, Food etc");
        try {
            execute(message);
            service.setUserState(id,"interests");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public String formatWeatherMessage(String weatherUpdatesJson) {
        if (weatherUpdatesJson == null || weatherUpdatesJson.isEmpty()) {
            return "Weather data not available.";
        }
        try {
            JSONArray dailyForecasts = new JSONArray(weatherUpdatesJson);
            StringBuilder sb = new StringBuilder("Weather forecast:\n");
            for (int i = 0; i < dailyForecasts.length(); i++) {
                JSONObject day = dailyForecasts.getJSONObject(i);
                String date = day.getString("date");
                double minTemp = day.getDouble("temp_min");
                double maxTemp = day.getDouble("temp_max");
                sb.append(String.format("%s: Min %.1f°C, Max %.1f°C\n", date, minTemp, maxTemp));
            }
            return sb.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return "Unable to parse weather data.";
        }
    }

    public String getHealthyTravelTips() {
        String[] tips = {
                "Stay hydrated! Drink plenty of water during your trip.",
                "Take short breaks to stretch and relax during long travels.",
                "Eat fresh fruits and veggies to keep your energy up.",
                "Use sunscreen to protect your skin outdoors.",
                "Get enough sleep to stay refreshed and enjoy your trip."
        };
        return String.join("\n", tips);
    }
    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage(chatId.toString(), text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    public void createTripPdf(String filePath, String tripPlanText) throws IOException {
        File file = new File(filePath);

        // Ensure parent directories exist
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean dirsCreated = parentDir.mkdirs();
            if (!dirsCreated) {
                throw new IOException("Failed to create directory: " + parentDir.getAbsolutePath());
            }
        }

        // Now create the PDF using iText7
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        document.add(new Paragraph("AI Generated Trip Plan\n\n"));
        document.add(new Paragraph(tripPlanText));

        document.close();
    }
    public void sendPdfDocument(Long chatId, String filePath) {
        SendDocument sendDocumentRequest = new SendDocument();
        sendDocumentRequest.setChatId(chatId.toString());
        sendDocumentRequest.setDocument(new InputFile(new File(filePath)));
        try {
            execute(sendDocumentRequest);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}