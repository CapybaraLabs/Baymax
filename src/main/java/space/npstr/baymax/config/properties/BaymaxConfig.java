package space.npstr.baymax.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by napster on 05.09.18.
 */
@Component
@ConfigurationProperties("baymax")
public class BaymaxConfig {

    private String discordToken = "";

    public String getDiscordToken() {
        return this.discordToken;
    }

    public void setDiscordToken(String discordToken) {
        this.discordToken = discordToken;
    }
}
