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

import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.requests.RequestFuture;
import space.npstr.baymax.db.TemporaryRoleService;
import space.npstr.baymax.helpdesk.Branch;
import space.npstr.baymax.helpdesk.Node;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created by napster on 05.09.18.
 */
public class UserDialogue {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserDialogue.class);

    private final EventWaiter eventWaiter;
    private final ShardManager shardManager;
    private final Map<String, Node> model;
    private final long userId;
    private final long channelId;
    private final RestActions restActions;
    private final TemporaryRoleService temporaryRoleService;
    private final EmojisNumbersParser emojisNumbersParser = new EmojisNumbersParser();
    private List<Long> messagesToCleanUp = new ArrayList<>();
    @Nullable
    private volatile EventWaiter.WaitingEvent<GuildMessageReceivedEvent> waitingEvent;
    private boolean done = false;

    public UserDialogue(EventWaiter eventWaiter, Map<String, Node> model, GuildMessageReceivedEvent event,
                        RestActions restActions, TemporaryRoleService temporaryRoleService) {

        this.eventWaiter = eventWaiter;
        this.shardManager = event.getJDA().asBot().getShardManager();
        this.model = model;
        this.userId = event.getAuthor().getIdLong();
        this.channelId = event.getChannel().getIdLong();
        this.restActions = restActions;
        this.temporaryRoleService = temporaryRoleService;

        this.messagesToCleanUp.add(event.getMessageIdLong());

        parseUserInput(event, model.get("root"));
    }

    public synchronized void done() {
        var we = this.waitingEvent;
        if (we != null) {
            we.cancel();
        }

        if (this.done) {
            return;
        }
        this.done = true;

        getTextChannel().ifPresent(textChannel -> {
            List<String> messageIdsAsStrings = this.messagesToCleanUp.stream()
                    .map(id -> Long.toString(id))
                    .collect(Collectors.toList());
            List<RequestFuture<Void>> requestFutures = textChannel.purgeMessagesById(messageIdsAsStrings);
            requestFutures.forEach(f -> f.whenComplete((__, t) -> {
                if (t != null) {
                    log.error("Failed to purge messages for user {} in channel {}", this.userId, this.channelId, t);
                }
            }));
        });
    }

    private Optional<TextChannel> getTextChannel() {
        return Optional.ofNullable(this.shardManager.getTextChannelById(this.channelId));
    }

    private void assignRole(TextChannel textChannel, long roleId) {
        Guild guild = textChannel.getGuild();
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            log.warn("Where did the role {} go?", roleId);
            return;
        }

        Member member = guild.getMemberById(this.userId);
        if (member == null) {
            log.warn("No member found for user {}", this.userId);
            return;
        }

        this.restActions.assignRole(guild, member, role);
        this.temporaryRoleService.setTemporaryRole(member.getUser(), role);
    }

    private void sendNode(Node node) {
        Optional<TextChannel> textChannelOpt = getTextChannel();
        if (textChannelOpt.isPresent()) {
            TextChannel textChannel = textChannelOpt.get();
            this.restActions.sendMessage(textChannel, asMessage(node))
                    .thenAccept(message -> this.messagesToCleanUp.add(message.getIdLong()))
                    .whenComplete((__, t) -> {
                        if (t != null) {
                            log.error("Failed to send message", t);
                        }
                    });

            Optional.ofNullable(node.getRoleId()).ifPresent(roleId -> assignRole(textChannel, roleId));
        } else {
            log.warn("Where did the channel {} go?", this.channelId);
        }

        this.waitingEvent = this.eventWaiter.waitForEvent(
                GuildMessageReceivedEvent.class,
                messageOfThisUser(),
                event -> this.parseUserInput(event, node),
                HelpDeskListener.EXPIRE_MINUTES, TimeUnit.MINUTES,
                this::done
        );
    }

    private void parseUserInput(GuildMessageReceivedEvent event, Node currentNode) {
        this.messagesToCleanUp.add(event.getMessageIdLong());
        String contentRaw = event.getMessage().getContentRaw();

        int numberPicked;
        try {
            numberPicked = Integer.parseInt(contentRaw);
        } catch (NumberFormatException e) {
            Optional<Integer> numberOpt = this.emojisNumbersParser.emojisToNumber(contentRaw);
            if (numberOpt.isEmpty()) {
                sendNode(currentNode); //todo better message?
                return;
            }
            numberPicked = numberOpt.get();
        } 

        numberPicked--; //correct for shown index starting at 1 instead of 0

        if (numberPicked < 0 || numberPicked > currentNode.getBranches().size()) {
            sendNode(currentNode); //todo better message?
            return;
        }

        Node nextNode;
        if (numberPicked == currentNode.getBranches().size()) {
            nextNode = this.model.get("root");
        } else {
            Branch branch = currentNode.getBranches().get(numberPicked);
            nextNode = this.model.get(branch.getTargetId());
        }
        sendNode(nextNode);
    }

    private Predicate<GuildMessageReceivedEvent> messageOfThisUser() {
        return event ->
                event.getAuthor().getIdLong() == this.userId
                        && event.getChannel().getIdLong() == this.channelId;
    }

    public static Message asMessage(Node node) {
        MessageBuilder mb = new MessageBuilder();
        EmojisNumbersParser emojisNumbersParser = new EmojisNumbersParser();

        mb.append("**").append(node.getTitle()).append("**\n\n");
        int bb = 1;
        for (Branch branch : node.getBranches()) {
            mb
                    .append(emojisNumbersParser.numberAsEmojis(bb++))
                    .append(" ")
                    .append(branch.getMessage())
                    .append("\n");
        }
        if (!"root".equals(node.getId())) {
            mb.append(emojisNumbersParser.numberAsEmojis(bb)).append(" ").append("Go back to the start.").append("\n");
        }

        return mb.build();
    }
}
