package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.*;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Grabber implements Grab {
    private final Properties cfg = new Properties();

    private static void context(JobExecutionContext context, String link, String subLink) {
        System.out.println(subLink);
        JobDataMap map = context
                .getJobDetail()
                .getJobDataMap();
        Store store = (Store) map.get("store");
        Parse parse = (Parse) map.get("parse");
        List<Post> list = parse.list(link);
        list.forEach(store::save);
        System.out.printf("Parsing %s is over...\n", subLink);
    }

    private JobDetail newWork(Class<? extends org.quartz.Job> jobClass, JobDataMap data) {
        return newJob(jobClass)
                .usingJobData(data)
                .build();
    }

    private Trigger newTrig(SimpleScheduleBuilder times) {
        return newTrigger()
                .startNow()
                .withSchedule(times)
                .build();
    }

    public Store store() {
        return new PsqlStore(cfg);
    }

    public Scheduler scheduler() throws SchedulerException {
        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.start();
        return scheduler;
    }

    public void cfg() throws IOException {
        try (InputStream in = Grabber.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            cfg.load(in);
        }
    }

    @Override
    public void init(Parse parse, Store store, Scheduler scheduler) throws SchedulerException {
        JobDataMap data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        JobDetail job = newWork(GrabJob.class, data);
        JobDetail work = newWork(WorkJob.class, data);
        SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInSeconds(Integer.parseInt(cfg.getProperty("time")))
                .repeatForever();
        Trigger triggerJob = newTrig(times);
        Trigger triggerWork = newTrig(times);
        scheduler.scheduleJob(job, triggerJob);
        scheduler.scheduleJob(work, triggerWork);
    }

    public static class GrabJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {
            String link = "https://career.habr.com/vacancies/java_developer?page=";
            var subLink = String.format("Site parsing %s", link.substring(0, 48));
            context(context, link, subLink);
        }
    }

    public static class WorkJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {
            String link = "https://career.habr.com/vacancies/programmist_python?page=";
            var subLink = String.format("Site parsing %s", link.substring(0, 52));
            context(context, link, subLink);
        }
    }

    public static void main(String[] args) throws SchedulerException, IOException {
        Grabber grab = new Grabber();
        grab.cfg();
        Scheduler scheduler = grab.scheduler();
        Store store = grab.store();
        grab.init(new HabrCareerParse(new HabrCareerDateTimeParser()), store, scheduler);
    }
}
