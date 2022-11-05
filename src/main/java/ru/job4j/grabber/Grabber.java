package ru.job4j.grabber;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import ru.job4j.grabber.utils.HabrCareerDateTimeParser;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class Grabber implements Grab {
    private static final String SOURCE_LINK
            = "https://career.habr.com/vacancies";
    private static final String JAVA_LINK
            = "https://career.habr.com/vacancies/java_developer?page=";
    private static final String PYTHON_LINK
            = "https://career.habr.com/vacancies/programmist_python?page=";
    private static final String JAVA_LITERAL
            = String.format("\"%s/java_developer\"", SOURCE_LINK);
    private static final String PYTHON_LITERAL
            = String.format("\"%s/programmist_python\"", SOURCE_LINK);

    private final Properties cfg = new Properties();

    private static void context(JobExecutionContext context, String link, String literal) {
        String subLink = String.format("Site parsing %s", literal);
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

    public void cfg() {
        try (InputStream in = Grabber.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            cfg.load(in);
        } catch (IOException io) {
            throw new IllegalArgumentException(io);
        }
    }

    public void web(Store store) {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(
                    Integer.parseInt(cfg.getProperty("port")))) {
                while (!server.isClosed()) {
                    Socket socket = server.accept();
                    try (OutputStream out = socket.getOutputStream()) {
                        out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
                        for (Post post : store.getAll()) {
                            out.write(post.toString().getBytes());
                            out.write(System.lineSeparator().getBytes());
                        }
                    } catch (IOException io) {
                        io.printStackTrace();
                    }
                }
            } catch (IOException io) {
                io.printStackTrace();
            }
        }).start();
    }

    @Override
    public void init(Parse parse, Store store, Scheduler scheduler) throws SchedulerException {
        JobDataMap data = new JobDataMap();
        data.put("store", store);
        data.put("parse", parse);
        JobDetail jobJava = newWork(JavaJob.class, data);
        JobDetail jobPython = newWork(PythonJob.class, data);
        SimpleScheduleBuilder times = simpleSchedule()
                .withIntervalInSeconds(Integer.parseInt(cfg.getProperty("time")))
                .repeatForever();
        Trigger triggerJob = newTrig(times);
        Trigger triggerWork = newTrig(times);
        scheduler.scheduleJob(jobJava, triggerJob);
        scheduler.scheduleJob(jobPython, triggerWork);
    }

    public static class JavaJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {
            context(context, JAVA_LINK, JAVA_LITERAL);
        }
    }

    public static class PythonJob implements Job {

        @Override
        public void execute(JobExecutionContext context) {
            context(context, PYTHON_LINK, PYTHON_LITERAL);
        }
    }

    public static void main(String[] args) throws SchedulerException {
        Grabber grab = new Grabber();
        grab.cfg();
        Scheduler scheduler = grab.scheduler();
        Store store = grab.store();
        grab.init(new HabrCareerParse(new HabrCareerDateTimeParser()), store, scheduler);
        grab.web(store);
    }
}
