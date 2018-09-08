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
        return this.version;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public String getArtifactId() {
        return this.artifactId;
    }

    public String getBuildNumber() {
        return this.buildNumber;
    }

    public long getBuildTime() {
        return this.buildTime;
    }

    public String getVersionBuild() {
        return this.version + "_" + this.buildNumber;
    }
}
