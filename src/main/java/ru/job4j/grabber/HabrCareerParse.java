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

    public static void main(String[] args) throws IOException {
        int pageMax = 5;
        String list = String.format("%s?page=", PAGE_LINK);
        for (int number = 1; number <= pageMax; number++) {
            String page = String.format("%s%d", list, number);
            Connection connection = Jsoup.connect(page);
            Document document = connection.get();
            Elements rows = document.select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                String vacancyName = titleElement.text();
                String vacancyDate = row.select(".basic-date").first().attr("datetime");
                LocalDateTime localDateTime = new HabrCareerDateTimeParser().parse(vacancyDate);
                String link = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                System.out.printf("%s %s %s%n", vacancyName, localDateTime, link);
            });
        }
    }
}