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

/**
 * Created by napster on 05.09.18.
 */
public class MissingTargetNodeException extends MalformedModelException {

    private final Node sourceNode;
    private final String targetNodeId;

    public MissingTargetNodeException(Node sourceNode, String targetNodeId) {
        super();
        this.sourceNode = sourceNode;
        this.targetNodeId = targetNodeId;
    }

    @Override
    public String getMessage() {
        return "Node " + this.sourceNode.getId() + "  has a branch referencing a non-existent node " + this.targetNodeId;
    }
}
