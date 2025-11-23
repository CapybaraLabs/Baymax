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

import java.net.URI;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by napster on 05.09.18.
 */
@ConfigurationProperties("baymax")
public record BaymaxConfig(
    String discordToken,
    int statusType,
    @Nullable String statusMessage,
    Set<Long> staffRoleIds,
    List<HelpDesk> helpDesks
) {

    @SuppressWarnings("ConstantValue")
    public BaymaxConfig {
        if (discordToken == null || discordToken.isBlank()) {
            throw new IllegalArgumentException("Discord token must not be blank");
        }
        if (staffRoleIds == null) {
            staffRoleIds = Set.of();
        }
        if (helpDesks == null) {
            helpDesks = List.of();
        }
    }

    public record HelpDesk(
        long channelId,
        String modelName,
        @Nullable URI modelUri
    ) {

        @SuppressWarnings("ConstantValue")
        public HelpDesk {
            if (channelId <= 0) {
                throw new IllegalArgumentException("Channel id must be positive");
            }
            if (modelName == null || modelName.isBlank()) {
                throw new IllegalArgumentException("Model name must not be blank");
            }
        }
    }
}
