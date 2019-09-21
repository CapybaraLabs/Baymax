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

package space.npstr.baymax.db;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.stereotype.Component;
import space.npstr.baymax.RestActions;
import space.npstr.baymax.ShardManagerHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Created by napster on 21.09.18.
 */
@Component
public class TemporaryRoleService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TemporaryRoleService.class);

    private static final int KEEP_ROLE_FOR_HOURS = 3;

    private final Database database;
    private final Supplier<ShardManager> shardManager;
    private final RestActions restActions;

    public TemporaryRoleService(Database database, ShardManagerHolder shardManagerHolder, RestActions restActions) {
        this.database = database;
        this.shardManager = shardManagerHolder;
        this.restActions = restActions;
        var cleaner = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "temporary-role-service"));

        cleaner.scheduleAtFixedRate(this::cleanUp, 1, 1, TimeUnit.MINUTES);
    }

    public void setTemporaryRole(User user, Role role) {
        long userId = user.getIdLong();
        long roleId = role.getIdLong();
        long guildId = role.getGuild().getIdLong();
        long until = OffsetDateTime.now()
                .plusHours(KEEP_ROLE_FOR_HOURS)
                .toInstant().toEpochMilli();

        //language=SQLite
        String existsSql = "SELECT EXISTS(SELECT * FROM temporary_role WHERE user_id = ? AND role_id = ?);";

        //language=SQLite
        String insertSql = "INSERT INTO temporary_role VALUES(?, ?, ?, ?);";
        //language=SQLite
        String updateSql = "UPDATE temporary_role SET guild_id = ?, until = ? WHERE user_id = ? AND role_id = ?;";

        try (Connection connection = this.database.getConnection()) {
            boolean exists;
            try (PreparedStatement statement = connection.prepareStatement(existsSql)) {
                statement.setLong(1, userId);
                statement.setLong(2, roleId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    exists = resultSet.getBoolean(1);
                }

            }
            String query = exists ? updateSql : insertSql;

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                if (exists) {
                    statement.setLong(1, guildId);
                    statement.setLong(2, until);
                    statement.setLong(3, userId);
                    statement.setLong(4, roleId);
                } else {
                    statement.setLong(1, userId);
                    statement.setLong(2, roleId);
                    statement.setLong(3, guildId);
                    statement.setLong(4, until);
                }
                statement.execute();
            }
        } catch (SQLException e) {
            log.error("Failed to set temporary role. User: {} Role: {}", user, role, e);
        }
    }

    private void cleanUp() {
        //avoid runnign this when were not ready
        if (this.shardManager.get().getShardCache().stream()
                .anyMatch(shard -> shard.getStatus() != JDA.Status.CONNECTED)) {
            return;
        }

        //language=SQLite
        String query = "SELECT * FROM temporary_role;";

        try (Connection connection = this.database.getConnection()) {
            List<TemporaryRoleId> toDelete = new ArrayList<>();
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery(query)) {
                    while (resultSet.next()) {
                        long until = resultSet.getLong(4);
                        if (System.currentTimeMillis() < until) {
                            continue;
                        }
                        long userId = resultSet.getLong(1);
                        long roleId = resultSet.getLong(2);
                        long guildId = resultSet.getLong(3);
                        Guild guild = this.shardManager.get().getGuildById(guildId);
                        if (guild == null) { //we left the guild. dont do anything, we might get readded
                            continue;
                        }
                        Role role = guild.getRoleById(roleId);
                        Member member = guild.getMemberById(userId);
                        if (role == null || member == null) { //role or member is gone. no point in keeping records on it
                            toDelete.add(new TemporaryRoleId(userId, roleId));
                            continue;
                        }

                        log.debug("Removing role {} from member {}", role, member);
                        this.restActions.removeRole(guild, member, role);

                        //remove the row
                        toDelete.add(new TemporaryRoleId(userId, roleId));
                    }
                }
            }
            //language=SQLite
            String deleteQuery = "DELETE FROM temporary_role WHERE user_id = ? AND role_id = ?;";
            try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
                for (TemporaryRoleId id : toDelete) {
                    log.debug("Deleting temporary role entry for user {} and role {}", id.userId, id.roleId);
                    statement.setLong(1, id.userId);
                    statement.setLong(2, id.roleId);
                    statement.execute();
                }
            }
        } catch (SQLException e) {
            log.error("Failed to clean up", e);
        }
    }

    //todo clean guild members of the roles?

    private static class TemporaryRoleId {
        private final long userId;
        private final long roleId;

        public TemporaryRoleId(long userId, long roleId) {
            this.userId = userId;
            this.roleId = roleId;
        }
    }
}
