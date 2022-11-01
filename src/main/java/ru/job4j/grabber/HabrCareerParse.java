package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;

public class HabrCareerParse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String
            .format("%s/vacancies/java_developer", SOURCE_LINK);
    private static final int PAGE_MAX = 5;

    private static Document getDoc(String link) {
        Document doc = null;
        try {
            Connection connection = Jsoup.connect(link);
            doc = connection.get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }

    private static String retrieveDescription(String link) {
        return getDoc(link).select(".style-ugc").text();
    }

    public static void main(String[] args) {
        String list = String.format("%s?page=", PAGE_LINK);
        for (int number = 1; number <= PAGE_MAX; number++) {
            String page = String.format("%s%d", list, number);
            Elements rows = getDoc(page).select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                String vacancyName = titleElement.text();
                String vacancyDate = row.select(".basic-date").first().attr("datetime");
                LocalDateTime localDateTime = new HabrCareerDateTimeParser().parse(vacancyDate);
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                String description = retrieveDescription(link);
                System.out.printf("%s %s %s %s%n", vacancyName, link, description, localDateTime);
            });
        }
    }
}