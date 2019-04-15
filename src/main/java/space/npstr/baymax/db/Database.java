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

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.springframework.stereotype.Component;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by napster on 21.09.18.
 */
@Component
public class Database {

    private static final String JDBC_URL = "jdbc:sqlite:baymax.sqlite";

    private final DataSource dataSource;

    public Database() {
        SQLiteConfig sqliteConfig = new SQLiteConfig();
        SQLiteDataSource sqliteDataSource = new SQLiteDataSource(sqliteConfig);
        sqliteDataSource.setUrl(JDBC_URL);
        migrate(sqliteDataSource);

        this.dataSource = sqliteDataSource;
    }

    public Connection getConnection() throws SQLException {
        return this.dataSource.getConnection();
    }

    private void migrate(DataSource dataSource) {
        Flyway flyway = new Flyway(Flyway.configure()
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("0"))
                .baselineDescription("Base Migration")
                .locations("db/migrations")
            .dataSource(dataSource)
        );
        flyway.migrate();
    }
}
