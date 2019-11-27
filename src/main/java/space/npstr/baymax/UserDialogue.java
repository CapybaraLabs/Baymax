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

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.sharding.ShardManager;
import space.npstr.baymax.db.TemporaryRoleService;
import space.npstr.baymax.helpdesk.Branch;
import space.npstr.baymax.helpdesk.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
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
    private final List<Long> messagesToCleanUp = new ArrayList<>();
    private final AtomicReference<EventWaiter.WaitingEvent<GuildMessageReceivedEvent>> waitingEvent = new AtomicReference<>();
    private boolean done = false;

    public UserDialogue(EventWaiter eventWaiter, Map<String, Node> model, GuildMessageReceivedEvent event,
                        RestActions restActions, TemporaryRoleService temporaryRoleService) {

        this.eventWaiter = eventWaiter;
        this.shardManager = Objects.requireNonNull(event.getJDA().getShardManager(), "Shard Manager required");
        this.model = model;
        this.userId = event.getAuthor().getIdLong();
        this.channelId = event.getChannel().getIdLong();
        this.restActions = restActions;
        this.temporaryRoleService = temporaryRoleService;

        this.messagesToCleanUp.add(event.getMessageIdLong());

        NodeContext nodeContext = new NodeContext(model.get("root"), Optional.empty());
        parseUserInput(event, nodeContext);
    }

    public synchronized void done() {
        var we = this.waitingEvent.get();
        if (we != null) {
            we.cancel();
        }

        if (this.done) {
            return;
        }
        this.done = true;

        getTextChannel().ifPresent(textChannel -> {
            List<String> messageIdsAsStrings = this.messagesToCleanUp.stream()
                    .map(Number::toString)
                    .collect(Collectors.toList());
            List<CompletableFuture<Void>> requestFutures = textChannel.purgeMessagesById(messageIdsAsStrings);
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

    private void sendNode(NodeContext nodeContext) {
        Optional<TextChannel> textChannelOpt = getTextChannel();
        if (textChannelOpt.isPresent()) {
            TextChannel textChannel = textChannelOpt.get();
            this.restActions.sendMessage(textChannel, asMessage(nodeContext))
                    .thenAccept(message -> this.messagesToCleanUp.add(message.getIdLong()))
                    .whenComplete((__, t) -> {
                        if (t != null) {
                            log.error("Failed to send message", t);
                        }
                    });

            Optional.ofNullable(nodeContext.getNode().getRoleId()).ifPresent(roleId -> assignRole(textChannel, roleId));
        } else {
            log.warn("Where did the channel {} go?", this.channelId);
        }

        this.waitingEvent.set(this.eventWaiter.waitForEvent(
                GuildMessageReceivedEvent.class,
                messageOfThisUser(),
                event -> this.parseUserInput(event, nodeContext),
                HelpDeskListener.EXPIRE_MINUTES, TimeUnit.MINUTES,
                this::done
        ));
    }

    private void parseUserInput(GuildMessageReceivedEvent event, NodeContext currentNodeContext) {
        this.messagesToCleanUp.add(event.getMessageIdLong());
        String contentRaw = event.getMessage().getContentRaw();
        Node currentNode = currentNodeContext.getNode();
        Optional<NodeContext> previousNodeContext = currentNodeContext.getPreviousNodeContext();

        int numberPicked;
        try {
            numberPicked = Integer.parseInt(contentRaw);
        } catch (NumberFormatException e) {
            Optional<Integer> numberOpt = this.emojisNumbersParser.emojisToNumber(contentRaw);
            if (numberOpt.isEmpty()) {
                sendNode(currentNodeContext); //todo better message?
                return;
            }
            numberPicked = numberOpt.get();
        }

        numberPicked--; //correct for shown index starting at 1 instead of 0

        // additional navigational numbers shown below the real numbers
        // +1 = go back
        // +2 = go to start
        int goBack;
        int goToStart;
        goBack = goToStart = currentNode.getBranches().size();
        if (previousNodeContext.isPresent()) {
            goToStart++;
        }

        if (numberPicked < 0 || numberPicked > goToStart) {
            sendNode(currentNodeContext); //todo better message?
            return;
        }

        NodeContext nextNodeContext;
        if (numberPicked == goToStart || numberPicked == goBack) {
            if (numberPicked == goBack && previousNodeContext.isPresent()) {
                nextNodeContext = previousNodeContext.get();
            } else {
                Node rootNode = this.model.get("root");
                nextNodeContext = new NodeContext(rootNode, Optional.empty());
            }
        } else {
            Branch branch = currentNode.getBranches().get(numberPicked);
            Node nextNode = this.model.get(branch.getTargetId());
            nextNodeContext = new NodeContext(nextNode, Optional.of(currentNodeContext));
        }
        sendNode(nextNodeContext);
    }

    private Predicate<GuildMessageReceivedEvent> messageOfThisUser() {
        return event ->
                event.getAuthor().getIdLong() == this.userId
                        && event.getChannel().getIdLong() == this.channelId;
    }

    public static Message asMessage(NodeContext nodeContext) {
        MessageBuilder mb = new MessageBuilder();
        EmojisNumbersParser emojisNumbersParser = new EmojisNumbersParser();
        Node node = nodeContext.getNode();

        mb.append("**").append(node.getTitle()).append("**\n\n");
        int bb = 1;
        for (Branch branch : node.getBranches()) {
            mb
                    .append(emojisNumbersParser.numberAsEmojis(bb++))
                    .append(" ")
                    .append(branch.getMessage())
                    .append("\n");
        }
        mb.append("\n");
        if ("root".equals(node.getId())) {
            mb.append("Say a number to start.").append("\n");
        } else {
            Optional<NodeContext> previousNodeContext = nodeContext.getPreviousNodeContext();
            if (previousNodeContext.isPresent()) {
                mb.append(emojisNumbersParser.numberAsEmojis(bb++)).append(" ").append("Go back one step.").append("\n");
            }
            mb.append(emojisNumbersParser.numberAsEmojis(bb)).append(" ").append("Go back to the start.").append("\n");
        }

        return mb.build();
    }
}
