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

package space.npstr.baymax.helpdesk;

import java.util.List;
import org.springframework.lang.Nullable;

/**
 * Created by napster on 05.09.18.
 */
public interface Node {

    /**
     * @return The id of this node
     */
    String getId();

    /**
     * @return The title of this node.
     */
    String getTitle();

    /**
     * @return A possible role associated with reaching this node that will be assigned to the user.
     * <p>
     * NOTE: We can't use Optional due to SnakeYaml aiming for Java 6 compatibility :clap:
     */
    @Nullable
    Long getRoleId();

    /**
     * @return A list of branches connecting this node to 0-n other nodes in a unidirectional way
     */
    List<? extends Branch> getBranches();
}
