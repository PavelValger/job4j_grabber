package ru.job4j.grabber;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import ru.job4j.grabber.utils.DateTimeParser;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HabrCareerParse implements Parse {
    private static final String SOURCE_LINK = "https://career.habr.com";
    private static final String PAGE_LINK = String
            .format("%s/vacancies/java_developer", SOURCE_LINK);
    private static final int PAGE_MAX = 5;
    private final DateTimeParser dateTimeParser;

    public HabrCareerParse(DateTimeParser dateTimeParser) {
        this.dateTimeParser = dateTimeParser;
    }

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

    private String retrieveDescription(String link) {
        return getDoc(link).selectFirst(".style-ugc").text();
    }

    @Override
    public List<Post> list(String link) {
        List<Post> list = new ArrayList<>();
        for (int number = 1; number <= PAGE_MAX; number++) {
            String page = String.format("%s%d", link, number);
            Elements rows = getDoc(page).select(".vacancy-card__inner");
            rows.forEach(row -> {
                Element titleElement = row.select(".vacancy-card__title").first();
                Element linkElement = titleElement.child(0);
                String vacancyName = titleElement.text();
                String vacancyDate = row.select(".basic-date").first().attr("datetime");
                LocalDateTime localDateTime = dateTimeParser.parse(vacancyDate);
                String subLink = String.format("%s%s", SOURCE_LINK, linkElement.attr("href"));
                String description = retrieveDescription(subLink);
                Post post = new Post(vacancyName, subLink, description, localDateTime);
                list.add(post);
            });
        }
        return list;
    }

    public static void main(String[] args) {
        HabrCareerParse habrCareerParse = new HabrCareerParse(new HabrCareerDateTimeParser());
        List<Post> list = habrCareerParse.list(String.format("%s?page=", PAGE_LINK));
        System.out.println(habrCareerParse.getDateTimeParser());
        System.out.println(list.get(24));
        System.out.println(list.get(24).getDescription());
    }

    public DateTimeParser getDateTimeParser() {
        return dateTimeParser;
    }
}