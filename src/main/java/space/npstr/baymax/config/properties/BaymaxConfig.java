package space.npstr.baymax.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Created by napster on 05.09.18.
 */
@Component
@ConfigurationProperties("baymax")
public class BaymaxConfig {

    private String discordToken = "";
    private List<HelpDesk> helpDesks = Collections.emptyList();

    public String getDiscordToken() {
        return this.discordToken;
    }

    public void setDiscordToken(String discordToken) {
        this.discordToken = discordToken;
    }

    public List<HelpDesk> getHelpDesks() {
        return this.helpDesks;
    }

    public void setHelpDesks(List<HelpDesk> helpDesks) {
        this.helpDesks = helpDesks;
    }

    public static class HelpDesk {

        private long channelId;
        private String modelName = "";

        public long getChannelId() {
            return this.channelId;
        }

        public void setChannelId(long channelId) {
            this.channelId = channelId;
        }

        public String getModelName() {
            return this.modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }
    }
}
