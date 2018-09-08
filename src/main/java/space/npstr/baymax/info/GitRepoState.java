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
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Created by napster on 05.09.18.
 */
public class GitRepoState {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GitRepoState.class);

    public static GitRepoState getGitRepositoryState() {
        return GitRepoStateHolder.INSTANCE;
    }

    //holder pattern
    private static final class GitRepoStateHolder {
        private static final GitRepoState INSTANCE = new GitRepoState("git.properties");
    }

    public final String branch;
    public final String commitId;
    public final String commitIdAbbrev;
    public final String commitUserName;
    public final String commitUserEmail;
    public final String commitMessageFull;
    public final String commitMessageShort;
    public final long commitTime; //epoch seconds

    @SuppressWarnings("ConstantConditions")
    public GitRepoState(String propsName) {

        Properties properties = new Properties();
        try {
            properties.load(GitRepoState.class.getClassLoader().getResourceAsStream(propsName));
        } catch (NullPointerException | IOException e) {
            log.info("Failed to load git repo information", e); //need to build with build tool to get them
        }

        this.branch = String.valueOf(properties.getOrDefault("git.branch", ""));
        this.commitId = String.valueOf(properties.getOrDefault("git.commit.id", ""));
        this.commitIdAbbrev = String.valueOf(properties.getOrDefault("git.commit.id.abbrev", ""));
        this.commitUserName = String.valueOf(properties.getOrDefault("git.commit.user.name", ""));
        this.commitUserEmail = String.valueOf(properties.getOrDefault("git.commit.user.email", ""));
        this.commitMessageFull = String.valueOf(properties.getOrDefault("git.commit.message.full", ""));
        this.commitMessageShort = String.valueOf(properties.getOrDefault("git.commit.message.short", ""));
        final String time = String.valueOf(properties.get("git.commit.time"));
        if (time == null) {
            this.commitTime = 0;
        } else {
            this.commitTime = OffsetDateTime.from(getDtf().parse(time)).toEpochSecond();
        }
    }

    //DateTimeFormatter is not threadsafe
    private DateTimeFormatter getDtf() {
        // https://github.com/n0mer/gradle-git-properties/issues/71
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    }

}
