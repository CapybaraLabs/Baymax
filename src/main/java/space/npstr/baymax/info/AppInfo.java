package space.npstr.baymax.info;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by napster on 05.09.18.
 */
public class AppInfo {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AppInfo.class);

    public static AppInfo getAppInfo() {
        return AppInfoHolder.INSTANCE;
    }

    //holder pattern
    private static final class AppInfoHolder {
        private static final AppInfo INSTANCE = new AppInfo();
    }

    private final String version;
    private final String groupId;
    private final String artifactId;
    private final String buildNumber;
    private final long buildTime;

    private AppInfo() {
        InputStream resourceAsStream = this.getClass().getResourceAsStream("/app.properties");
        Properties prop = new Properties();
        try {
            prop.load(resourceAsStream);
        } catch (IOException e) {
            log.error("Failed to load app.properties", e);
        }
        this.version = prop.getProperty("version");
        this.groupId = prop.getProperty("groupId");
        this.artifactId = prop.getProperty("artifactId");
        this.buildNumber = prop.getProperty("buildNumber");
        this.buildTime = Long.parseLong(prop.getProperty("buildTime"));
    }

    public String getVersion() {
        return version;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public long getBuildTime() {
        return buildTime;
    }

    public String getVersionBuild() {
        return this.version + "_" + this.buildNumber;
    }
}
