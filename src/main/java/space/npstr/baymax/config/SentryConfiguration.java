package space.npstr.baymax.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.filter.ThresholdFilter;
import io.sentry.Sentry;
import io.sentry.logback.SentryAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import space.npstr.baymax.config.properties.SentryConfig;
import space.npstr.baymax.info.GitRepoState;

/**
 * Created by napster on 09.05.18.
 */
@Configuration
public class SentryConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SentryConfiguration.class);

    private static final String SENTRY_APPENDER_NAME = "SENTRY";

    public SentryConfiguration(SentryConfig sentryConfig) {

        String dsn = sentryConfig.getDsn();

        //noinspection ConstantConditions
        if (dsn != null && !dsn.isEmpty()) {
            turnOn(dsn);
        } else {
            turnOff();
        }

    }

    private void turnOn(String dsn) {
        log.info("Turning on sentry");
        Sentry.init(dsn).setRelease(GitRepoState.getGitRepositoryState().commitId);
        getSentryLogbackAppender().start();
    }


    private static void turnOff() {
        log.warn("Turning off sentry");
        Sentry.close();
        getSentryLogbackAppender().stop();
    }

    //programmatically creates a sentry appender if it doesn't exist yet
    private static synchronized SentryAppender getSentryLogbackAppender() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger root = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

        SentryAppender sentryAppender = (SentryAppender) root.getAppender(SENTRY_APPENDER_NAME);
        if (sentryAppender == null) {
            sentryAppender = new SentryAppender();
            sentryAppender.setName(SENTRY_APPENDER_NAME);

            ThresholdFilter warningsOrAboveFilter = new ThresholdFilter();
            warningsOrAboveFilter.setLevel(Level.WARN.levelStr);
            warningsOrAboveFilter.start();
            sentryAppender.addFilter(warningsOrAboveFilter);

            sentryAppender.setContext(loggerContext);
            root.addAppender(sentryAppender);
        }
        return sentryAppender;
    }
}
