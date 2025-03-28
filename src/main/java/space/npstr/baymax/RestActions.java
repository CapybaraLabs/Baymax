/*
 * Copyright (C) 2018-2022 Dennis Neufeld
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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.springframework.stereotype.Component;

/**
 * Created by napster on 21.09.18.
 */
@Component
public class RestActions {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RestActions.class);

    public void assignRole(Guild guild, Member member, Role role) {
        try {
            guild.addRoleToMember(member, role).queue();
        } catch (InsufficientPermissionException e) {
            log.error("Can't assign role {} due to missing permission {}", role, e.getPermission(), e);
        } catch (HierarchyException e) {
            log.error("Can't assign role {} due to hierarchy issue", role, e);
        }
    }

    public void removeRole(Guild guild, Member member, Role role) {
        try {
            guild.removeRoleFromMember(member, role).queue();
        } catch (InsufficientPermissionException e) {
            log.error("Can't remove role {} due to missing permission {}", role, e.getPermission(), e);
        } catch (HierarchyException e) {
            log.error("Can't remove role {} due to hierarchy issue", role, e);
        }
    }

    public CompletionStage<Message> sendMessage(MessageChannel channel, MessageCreateData message) {
        return channel.sendMessage(message).submit()
            .thenApply(Function.identity()); //avoid JDA's Promise#toCompletableFuture's UnsupportedOperationException
    }

    public CompletionStage<Void> deleteMessageAfter(Message message, Duration after) {
        return message.delete()
                .submitAfter(after.toMillis(), TimeUnit.MILLISECONDS);
    }

    public CompletionStage<Void> purgeChannel(MessageChannel channel) {
        return fetchAllMessages(channel.getHistory())
            .thenApply(channel::purgeMessages)
            .thenCompose(requestFutures -> CompletableFuture.allOf(requestFutures.toArray(new CompletableFuture[0])));
    }

    private CompletionStage<List<Message>> fetchAllMessages(MessageHistory history) {
        return history.retrievePast(100).submit()
                .thenApply(Function.identity()) //avoid JDA's Promise#toCompletableFuture's UnsupportedOperationException
                .thenCompose(
                        messages -> {
                            if (!messages.isEmpty()) {
                                return fetchAllMessages(history);
                            }
                            return CompletableFuture.completedStage(history.getRetrievedHistory());
                        });
    }
}
