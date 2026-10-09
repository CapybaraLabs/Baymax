/*
 * Copyright (C) 2026 Dennis Neufeld
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

public class TooManyBranchesException extends MalformedModelException {

    private final Node node;
    private final int maxBranches;

    public TooManyBranchesException(Node node, int maxBranches) {
        super();
        this.node = node;
        this.maxBranches = maxBranches;
    }

    @Override
    public String getMessage() {
        return "Node " + this.node.getId() + " has " + this.node.getBranches().size() + " branches, but at most "
            + this.maxBranches + " are supported";
    }
}
