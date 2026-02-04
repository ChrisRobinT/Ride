package com.crt.fujitsu.ride_pricing_app.service_logic;

import com.crt.fujitsu.ride_pricing_app.model.WeatherData;
import com.crt.fujitsu.ride_pricing_app.repository.WeatherDataRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApiWeather {

    private final WeatherDataRepository weatherDataRepository;
    private final RestTemplate restTemplate;

    // Your API key from WeatherAPI.com
    private static final String API_KEY = "20ac311827124d0ba50125028250404";
    // Base URL for WeatherAPI.com's current weather endpoint
    private static final String BASE_URL = "http://api.weatherapi.com/v1/current.xml?key=" + API_KEY + "&aqi=no&q=";

    public ApiWeather(WeatherDataRepository weatherDataRepository, RestTemplate restTemplate) {
        this.weatherDataRepository = weatherDataRepository;
        this.restTemplate = restTemplate;
    }

    // This method runs periodically (every 5 seconds for testing; adjust fixedRate or use a cron expression for production)
    @Scheduled(fixedRate = 45000)
    public void importWeatherData() {
        System.out.println("importWeatherData called");
        try {
            // City query -> canonical station name for DB (use lat,long for Pärnu to avoid encoding issues)
            String[][] cityQueries = {
                {"Tallinn", "Tallinn"},
                {"Tartu_Estonia", "Tartu"},
                {"58.3858,24.4971", "Pärnu"}  // Pärnu coordinates
            };
            List<WeatherData> weatherDataList = new ArrayList<>();
            LocalDateTime observationTime = LocalDateTime.now();

            for (String[] cityEntry : cityQueries) {
                String cityQuery = cityEntry[0];
                String canonicalStationName = cityEntry[1];
                String url = BASE_URL + cityQuery;

                HttpHeaders headers = new HttpHeaders();
                headers.set("Accept", "application/xml");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                String xmlData = response.getBody();

                // Parse the XML response
                Document document = parseXmlString(xmlData);

                // Use canonical name so lookups work regardless of API response encoding
                String stationName = canonicalStationName;
                Double airTemperature = null;
                Double windSpeed = null;
                String phenomenon = "";

                // stationName uses canonical name for consistent DB lookups

                // Extract current weather details (nested in <current>) - with null check
                Element currentElement = getElementByTagName(document, "current");
                if (currentElement != null) {
                    String tempCStr = getElementTextContent(currentElement, "temp_c");
                    airTemperature = parseDoubleOrNull(tempCStr);

                    String windKphStr = getElementTextContent(currentElement, "wind_kph");
                    Double windSpeedKph = parseDoubleOrNull(windKphStr);
                    // Convert wind speed from km/h to m/s (1 km/h ≈ 0.27778 m/s)
                    windSpeed = (windSpeedKph != null) ? windSpeedKph * 0.27778 : null;

                    // Try to get wind_mph if wind_kph is not available
                    if (windSpeed == null) {
                        String windMphStr = getElementTextContent(currentElement, "wind_mph");
                        Double windSpeedMph = parseDoubleOrNull(windMphStr);
                        // Convert wind speed from mph to m/s (1 mph ≈ 0.44704 m/s)
                        windSpeed = (windSpeedMph != null) ? windSpeedMph * 0.44704 : null;
                    }

                    // Extract weather condition (nested within <condition> inside <current>)
                    Element conditionElement = getElementByTagName(currentElement, "condition");
                    if (conditionElement != null) {
                        String text = getElementTextContent(conditionElement, "text");
                        if (text != null && !text.isEmpty()) {
                            phenomenon = text;
                        }
                    }
                }

                // Create a new WeatherData object
                WeatherData data = new WeatherData(
                        stationName,
                        "",  // WeatherAPI does not provide a WMO code; leave empty or set accordingly.
                        airTemperature,
                        windSpeed,
                        phenomenon,
                        observationTime
                );

                weatherDataList.add(data);
            }
            weatherDataRepository.saveAll(weatherDataList);
        } catch (Exception e) {
            System.err.println("Error importing weather data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Document parseXmlString(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlString)));
    }

    private Element getElementByTagName(Document document, String tagName) {
        NodeList nodeList = document.getElementsByTagName(tagName);
        return nodeList.getLength() > 0 ? (Element) nodeList.item(0) : null;
    }

    private Element getElementByTagName(Element parent, String tagName) {
        if (parent == null) return null;
        NodeList nodeList = parent.getElementsByTagName(tagName);
        return nodeList.getLength() > 0 ? (Element) nodeList.item(0) : null;
    }

    private String getElementTextContent(Element parent, String tagName) {
        if (parent == null) return null;
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    private Double parseDoubleOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}