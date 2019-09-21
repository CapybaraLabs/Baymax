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

package space.npstr.baymax;

import com.google.common.base.Suppliers;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.aop.framework.Advised;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Created by napster on 21.09.18.
 * <p>
 * Avoid circular dependencies between the very central ShardManager bean and other beans
 */
@Component
public class ShardManagerHolder implements Supplier<ShardManager> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ShardManagerHolder.class);

    private final Supplier<ShardManager> lazyShardManager;

    public ShardManagerHolder(@Lazy ShardManager shardManagerProxy) {

        //unwrap the spring proxy of the lazy bean
        // we require the raw bean, because the proxy will error out when accessed during shutdown hooks, but
        // we manage the lifecycle of the shardManager singleton ourselves, so we don't need spring refusing us to serve
        // a perfectly fine bean during shutdown hooks.
        this.lazyShardManager = Suppliers.memoize(() -> {
            try {
                ShardManager target = (ShardManager) ((Advised) shardManagerProxy).getTargetSource().getTarget();
                if (target == null) {
                    throw new IllegalArgumentException("Failed to unproxy the lazy proxy of the shard manager");
                }
                return target;
            } catch (Exception e) {
                log.error("Failed to unproxy the shard manager", e);
                //this should not happen but if it does, just work with the proxy. however we might error out during
                // execution of shutdown handlers that rely on fetching jdaentities
                return shardManagerProxy;
            }
        });
    }

    @Override
    public ShardManager get() {
        return this.lazyShardManager.get();
    }
}
