package space.npstr.baymax;

import net.dv8tion.jda.core.JDAInfo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import space.npstr.baymax.info.AppInfo;
import space.npstr.baymax.info.GitRepoState;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Created by napster on 05.09.18.
 */
@SpringBootApplication
public class Launcher implements ApplicationRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Launcher.class);

    public static void main(String[] args) {
        //just post the info to the console
        if (args.length > 0 &&
                (args[0].equalsIgnoreCase("-v")
                        || args[0].equalsIgnoreCase("--version")
                        || args[0].equalsIgnoreCase("-version"))) {
            System.out.println("Version flag detected. Printing version info, then exiting.");
            System.out.println(getVersionInfo());
            System.out.println("Version info printed, exiting.");
            return;
        }

        System.setProperty("spring.config.name", "baymax");
        SpringApplication app = new SpringApplication(Launcher.class);
        app.addListeners(
                event -> {
                    if (event instanceof ApplicationEnvironmentPreparedEvent) {
                        log.info(getVersionInfo());
                    }
                },
                event -> {
                    if (event instanceof ApplicationFailedEvent) {
                        ApplicationFailedEvent failed = (ApplicationFailedEvent) event;
                        log.error("Application failed", failed.getException());
                    }
                }
        );
        app.run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

    }

    private static String getVersionInfo() {
        //copypasta'd from http://textart4u.blogspot.com/2014/10/disney-baymax-face-text-art-copy-paste.html
        String baymax
                = "¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶\n"
                + "¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶\n"
                + "¶¶¶¶¶¶¶¶¶___________________¶¶¶¶¶¶¶¶¶\n"
                + "¶¶¶¶¶¶_________________________¶¶¶¶¶¶\n"
                + "¶¶¶¶_____________________________¶¶¶¶\n"
                + "¶¶¶______¶¶¶_____________¶¶¶______¶¶¶\n"
                + "¶¶______¶¶¶¶¶___________¶¶¶¶¶______¶¶\n"
                + "¶¶______¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶______¶¶\n"
                + "¶¶_______¶¶¶_____________¶¶¶_______¶¶\n"
                + "¶¶¶_______________________________¶¶¶\n"
                + "¶¶¶¶_____________________________¶¶¶¶\n"
                + "¶¶¶¶¶¶_________________________¶¶¶¶¶¶\n"
                + "¶¶¶¶¶¶¶¶_____________________¶¶¶¶¶¶¶¶\n"
                + "¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶\n"
                + "¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶\n";

        return "\n\n" + baymax
                + "\n"
                + "\n\tVersion:       " + AppInfo.getAppInfo().getVersion()
                + "\n\tBuild:         " + AppInfo.getAppInfo().getBuildNumber()
                + "\n\tBuild time:    " + asTimeInCentralEurope(AppInfo.getAppInfo().getBuildTime())
                + "\n\tCommit:        " + GitRepoState.getGitRepositoryState().commitIdAbbrev + " (" + GitRepoState.getGitRepositoryState().branch + ")"
                + "\n\tCommit time:   " + asTimeInCentralEurope(GitRepoState.getGitRepositoryState().commitTime * 1000)
                + "\n\tJVM:           " + System.getProperty("java.version")
                + "\n\tJDA:           " + JDAInfo.VERSION
                + "\n";
    }

    private static String asTimeInCentralEurope(final long epochMillis) {
        return timeInCentralEuropeFormatter().format(Instant.ofEpochMilli(epochMillis));
    }

    //DateTimeFormatter is not threadsafe
    private static DateTimeFormatter timeInCentralEuropeFormatter() {
        return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss z")
                .withZone(ZoneId.of("Europe/Berlin"));
    }
}
