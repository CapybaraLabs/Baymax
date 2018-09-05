package space.npstr.baymax.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by napster on 05.09.18.
 */
@Component
@ConfigurationProperties("sentry")
public class SentryConfig {

    private String dsn = "";

    public String getDsn() {
        return this.dsn;
    }

    public void setDsn(String dsn) {
        this.dsn = dsn;
    }
}
