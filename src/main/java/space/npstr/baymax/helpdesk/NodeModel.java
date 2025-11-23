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

import java.util.Collections;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Created by napster on 05.09.18.
 */
public class NodeModel implements Node {

    private String id = "";

    private String title = "";

    @Nullable
    private Long roleId = null;

    private List<BranchModel> branches = Collections.emptyList();

    @Override
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    @Nullable
    public Long getRoleId() {
        return this.roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    @Override
    public List<BranchModel> getBranches() {
        return this.branches;
    }

    public void setBranches(List<BranchModel> branches) {
        this.branches = branches;
    }
}
