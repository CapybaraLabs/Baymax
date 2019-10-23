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

package space.npstr.baymax.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Created by napster on 05.09.18.
 */
@Component
@ConfigurationProperties("baymax")
public class BaymaxConfig {

    private String discordToken = "";
    private int statusType = 0;
    private String statusMessage = "";
    private Set<Long> staffRoleIds = Collections.emptySet();
    private List<HelpDesk> helpDesks = Collections.emptyList();

    public String getDiscordToken() {
        return this.discordToken;
    }

    public void setDiscordToken(String discordToken) {
        this.discordToken = discordToken;
    }

    public int getStatusType() {
        return statusType;
    }

    public void setStatusType(int statusType) {
        this.statusType = statusType;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Set<Long> getStaffRoleIds() {
        return this.staffRoleIds;
    }

    public void setStaffRoleIds(Set<Long> staffRoleIds) {
        this.staffRoleIds = staffRoleIds;
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
        private Optional<URI> modelUri = Optional.empty();

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

        public Optional<URI> getModelUri() {
            return modelUri;
        }

        public void setModelUri(URI modelUri) {
            this.modelUri = Optional.of(modelUri);
        }
    }
}
