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

package space.npstr.baymax.helpdesk.exception;

import space.npstr.baymax.helpdesk.Node;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Created by napster on 05.09.18.
 */
public class UnreferencedNodesException extends MalformedModelException {

    private final Collection<Node> unreferencedNodes;

    public UnreferencedNodesException(Collection<Node> unreferencedNodes) {
        super();
        this.unreferencedNodes = unreferencedNodes;
    }

    @Override
    public String getMessage() {
        String nodes = String.join(", ",
                this.unreferencedNodes.stream().map(Node::getId).collect(Collectors.toList())
        );
        return "Nodes " + nodes + " have no reference leading to them from the root node.";
    }
}
