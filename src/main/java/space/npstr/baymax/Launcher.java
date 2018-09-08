/*
 * Copyright (C) 2018 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.baymax;

import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDAInfo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import space.npstr.baymax.info.AppInfo;
import space.npstr.baymax.info.GitRepoState;

import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 05.09.18.
 */
@SpringBootApplication
public class Launcher implements ApplicationRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Launcher.class);

    private final Thread shutdownHook;
    private volatile boolean shutdownHookAdded = false;
    private volatile boolean shutdownHookExecuted = false;

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

    public Launcher(ShardManager shardManager, ScheduledThreadPoolExecutor jdaThreadPool) {
        this.shutdownHook = new Thread(() -> {
            try {
                shutdown(shardManager, jdaThreadPool);
            } catch (Exception e) {
                log.error("Uncaught exception in shutdown hook", e);
            } finally {
                this.shutdownHookExecuted = true;
            }
        }, "shutdown-hook");
    }

    @Override
    public void run(ApplicationArguments args) {
        Runtime.getRuntime().addShutdownHook(this.shutdownHook);
        this.shutdownHookAdded = true;
    }

    @PreDestroy
    public void waitOnShutdownHook() {

        // This condition can happen when spring encountered an exception during start up and is tearing itself down,
        // but did not call System.exit, so out shutdown hooks are not being executed.
        // If spring is tearing itself down, we always want to exit the JVM, so we call System.exit manually here, so
        // our shutdown hooks will be run, and the loop below does not hang forever.
        if (!isShuttingDown()) {
            System.exit(1);
        }

        while (this.shutdownHookAdded && !this.shutdownHookExecuted) {
            log.info("Waiting on main shutdown hook to be done...");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        log.info("Main shutdown hook done! Proceeding.");
    }

    private static final Thread DUMMY_HOOK = new Thread();

    public static boolean isShuttingDown() {
        try {
            Runtime.getRuntime().addShutdownHook(DUMMY_HOOK);
            Runtime.getRuntime().removeShutdownHook(DUMMY_HOOK);
        } catch (IllegalStateException ignored) {
            return true;
        }
        return false;
    }

    private void shutdown(ShardManager shardManager, ScheduledThreadPoolExecutor jdaThreadPool) {
        //okHttpClient claims that a shutdown isn't necessary

        //shutdown JDA
        log.info("Shutting down shards");
        shardManager.shutdown();

        //shutdown executors
        log.info("Shutting down jda thread pool");
        final List<Runnable> jdaThreadPoolRunnables = jdaThreadPool.shutdownNow();
        log.info("{} jda thread pool runnables cancelled", jdaThreadPoolRunnables.size());

        try {
            jdaThreadPool.awaitTermination(30, TimeUnit.SECONDS);
            log.info("Jda thread pool terminated");
        } catch (final InterruptedException e) {
            log.warn("Interrupted while awaiting executors termination", e);
            Thread.currentThread().interrupt();
        }
    }

    private static String getVersionInfo() {
        //copypasta'd from http://textart4u.blogspot.com/2014/10/disney-baymax-face-text-art-copy-paste.html
        String baymax
                = "\t¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶\n"
                + "\t¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶\n"
                + "\t¶¶¶¶¶¶¶¶¶___________________¶¶¶¶¶¶¶¶¶\n"
                + "\t¶¶¶¶¶¶_________________________¶¶¶¶¶¶\n"
                + "\t¶¶¶¶_____________________________¶¶¶¶\n"
                + "\t¶¶¶______¶¶¶_____________¶¶¶______¶¶¶\n"
                + "\t¶¶______¶¶¶¶¶___________¶¶¶¶¶______¶¶\n"
                + "\t¶¶______¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶______¶¶\n"
                + "\t¶¶_______¶¶¶_____________¶¶¶_______¶¶\n"
                + "\t¶¶¶_______________________________¶¶¶\n"
                + "\t¶¶¶¶_____________________________¶¶¶¶\n"
                + "\t¶¶¶¶¶¶_________________________¶¶¶¶¶¶\n"
                + "\t¶¶¶¶¶¶¶¶_____________________¶¶¶¶¶¶¶¶\n"
                + "\t¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶\n"
                + "\t¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶¶\n";

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
