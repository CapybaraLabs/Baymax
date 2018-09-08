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

import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import space.npstr.baymax.EventWaiter;
import space.npstr.baymax.HelpDeskListener;
import space.npstr.baymax.config.properties.BaymaxConfig;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by napster on 05.09.18.
 */
@Configuration
public class ShardManagerConfiguration {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ShardManagerConfiguration.class);

    private static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER
            = (thread, throwable) -> log.error("Uncaught exception in thread {}", thread.getName(), throwable);

    @Bean
    public ScheduledThreadPoolExecutor jdaThreadPool() {
        AtomicInteger threadNumber = new AtomicInteger(0);
        return new ScheduledThreadPoolExecutor(50, r -> {
            Thread thread = new Thread(r, "jda-pool-t" + threadNumber.getAndIncrement());
            thread.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
            return thread;
        });
    }

    @Bean(destroyMethod = "") //we manage the lifecycle ourselves tyvm, see shutdown hook in the launcher
    public ShardManager shardManager(BaymaxConfig baymaxConfig, OkHttpClient.Builder httpClientBuilder,
                                     ScheduledThreadPoolExecutor jdaThreadPool, EventWaiter eventWaiter,
                                     HelpDeskListener helpDeskListener) {

        Game discordStatus = Game.playing("with Aki");

        DefaultShardManagerBuilder shardBuilder = new DefaultShardManagerBuilder()
                .setToken(baymaxConfig.getDiscordToken())
                .setGame(discordStatus)
                .addEventListeners(eventWaiter)
                .addEventListeners(helpDeskListener)
                .setHttpClientBuilder(httpClientBuilder
                        .retryOnConnectionFailure(false))
                .setEnableShutdownHook(false)
                .setRateLimitPool(jdaThreadPool, false)
                .setCallbackPool(jdaThreadPool, false)
                .setDisabledCacheFlags(EnumSet.of(CacheFlag.EMOTE, CacheFlag.GAME, CacheFlag.VOICE_STATE));

        try {
            return shardBuilder.build();
        } catch (LoginException e) {
            String message = "Could not login with provided token, probably invalid";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }
}
