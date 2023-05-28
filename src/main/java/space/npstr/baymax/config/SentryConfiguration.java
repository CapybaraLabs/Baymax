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

package space.npstr.baymax.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
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
        Sentry.init(options -> {
            options.setDsn(dsn);
            options.setRelease(GitRepoState.getGitRepositoryState().commitId);
        });
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
            sentryAppender.setMinimumEventLevel(Level.WARN);
            sentryAppender.setContext(loggerContext);
            root.addAppender(sentryAppender);
        }
        return sentryAppender;
    }
}
