/*
 * Copyright (C) 2018-2026 Dennis Neufeld
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

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.error.YAMLException;
import space.npstr.baymax.config.properties.BaymaxConfig;
import space.npstr.baymax.db.TemporaryRoleService;
import space.npstr.baymax.helpdesk.Node;
import space.npstr.baymax.helpdesk.exception.MalformedModelException;

/**
 * Created by napster on 05.09.18.
 */
@Component
public class HelpDeskListener extends ListenerAdapter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(HelpDeskListener.class);

    private static final String COMMAND_NAME = "helpdesk";

    private final ModelLoader modelLoader;
    private final BaymaxConfig baymaxConfig;
    private final RestActions restActions;
    private final TemporaryRoleService temporaryRoleService;

    public HelpDeskListener(ModelLoader modelLoader, BaymaxConfig baymaxConfig,
                            RestActions restActions, TemporaryRoleService temporaryRoleService) {

        this.modelLoader = modelLoader;
        this.baymaxConfig = baymaxConfig;
        this.restActions = restActions;
        this.temporaryRoleService = temporaryRoleService;
    }

    /**
     * Help desk channels should only contain the root message. Anything else posted there gets cleaned up.
     */
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (findHelpDesk(event.getChannel().getIdLong()).isEmpty()) {
            return;
        }
        if (event.getAuthor().getIdLong() == event.getJDA().getSelfUser().getIdLong()) {
            return;
        }

        restActions.deleteMessageAfter(event.getMessage(), Duration.ofSeconds(5))
            .whenComplete((__, t) -> {
                if (t != null) {
                    log.error("Failed to delete message in channel {}", event.getChannel(), t);
                }
            });
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        Optional<List<Integer>> pathOpt = HelpDeskMenu.parseButtonId(event.getComponentId());
        if (pathOpt.isEmpty()) {
            return;
        }
        var helpDeskOpt = findHelpDesk(event.getChannelIdLong());
        if (helpDeskOpt.isEmpty()) {
            event.reply("This help desk is not active anymore.").setEphemeral(true).queue();
            return;
        }
        var helpDesk = helpDeskOpt.get();
        var model = this.modelLoader.getModel(helpDesk.modelName(), helpDesk.modelUri());

        List<Integer> path = pathOpt.get();
        Optional<Node> nodeOpt = HelpDeskMenu.resolve(model, path);
        if (nodeOpt.isEmpty()) { // the model changed since this button was created
            path = List.of();
            nodeOpt = HelpDeskMenu.resolve(model, path);
        }
        MessageCreateData message = HelpDeskMenu.render(model, path);

        // Clicks on the public root message open a private dialogue, clicks inside of it update it in place
        if (event.getMessage().isEphemeral()) {
            event.editMessage(MessageEditData.fromCreateData(message)).queue();
        } else {
            event.reply(message).setEphemeral(true).queue();
        }

        Long roleId = nodeOpt.orElseThrow().getRoleId();
        Member member = event.getMember();
        if (roleId != null && member != null) {
            assignRole(member, roleId);
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!COMMAND_NAME.equals(event.getName())) {
            return;
        }
        Member member = event.getMember();
        if (member == null || !isStaff(member)) {
            event.reply("Only staff members can do that.").setEphemeral(true).queue();
            return;
        }
        var helpDeskOpt = findHelpDesk(event.getChannelIdLong());
        if (helpDeskOpt.isEmpty() || !(event.getChannel() instanceof TextChannel channel)) {
            event.reply("This channel is not a help desk.").setEphemeral(true).queue();
            return;
        }
        var helpDesk = helpDeskOpt.get();

        event.deferReply(true).queue(); // loading a model may take a while
        String subcommand = Objects.requireNonNullElse(event.getSubcommandName(), "");
        switch (subcommand) {
            case "init" -> {
                init(channel, helpDesk.modelName(), helpDesk.modelUri());
                event.getHook().editOriginal("Help desk initialized.").queue();
            }
            case "reload" -> {
                try {
                    var reloadedModel = this.modelLoader.attemptReload(helpDesk.modelName(), helpDesk.modelUri());
                    init(channel, reloadedModel);
                    event.getHook().editOriginal("Model reloaded.").queue();
                } catch (MalformedModelException | YAMLException e) {
                    event.getHook().editOriginal("Failed to load model due to: **" + e.getMessage() + "**").queue();
                }
            }
            default -> event.getHook().editOriginal("Unknown subcommand " + subcommand).queue();
        }
    }

    // revisit for when there is more than one shard
    @Override
    public void onReady(ReadyEvent event) {
        //1. Register commands
        //2. Clean up the channel
        //3. Post the root message

        ShardManager shardManager = Objects.requireNonNull(event.getJDA().getShardManager(), "Shard manager required");
        List<TextChannel> channels = this.baymaxConfig.helpDesks().stream()
            .map(helpDesk -> {
                TextChannel channel = shardManager.getTextChannelById(helpDesk.channelId());
                if (channel == null) {
                    log.warn("Failed to find and setup configured help desk channel {}", helpDesk.channelId());
                }
                return channel;
            })
            .filter(Objects::nonNull)
            .toList();

        channels.stream().map(TextChannel::getGuild).distinct().forEach(this::registerCommands);

        for (BaymaxConfig.HelpDesk helpDesk : this.baymaxConfig.helpDesks()) {
            channels.stream()
                .filter(channel -> channel.getIdLong() == helpDesk.channelId())
                .findAny()
                .ifPresent(channel -> init(channel, helpDesk.modelName(), helpDesk.modelUri()));
        }
    }

    private void registerCommands(Guild guild) {
        guild.updateCommands()
            .addCommands(Commands.slash(COMMAND_NAME, "Manage the help desk in this channel")
                .addSubcommands(
                    new SubcommandData("init", "Clean up the channel and post the help desk"),
                    new SubcommandData("reload", "Reload the model and post the help desk")
                ))
            .queue(
                __ -> log.info("Registered commands in guild {}", guild),
                t -> log.error("Failed to register commands in guild {}", guild, t)
            );
    }

    private void init(TextChannel channel, String modelName, @Nullable URI modelUri) {
        init(channel, this.modelLoader.getModel(modelName, modelUri));
    }

    private void init(TextChannel channel, Map<String, Node> model) {
        try {
            this.restActions.purgeChannel(channel)
                .exceptionally(t -> {
                    log.error("Failed to purge messages for init in channel {}", channel, t);
                    return null; //Void
                })
                .thenCompose(__ -> this.restActions.sendMessage(channel, HelpDeskMenu.render(model, List.of())))
                    .whenComplete((__, t) -> {
                        if (t != null) {
                            log.error("Failed to send init message in channel {}", channel, t);
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to purge channel {}", channel, e);
        }
    }

    private void assignRole(Member member, long roleId) {
        Guild guild = member.getGuild();
        Role role = guild.getRoleById(roleId);
        if (role == null) {
            log.warn("Where did the role {} go?", roleId);
            return;
        }

        this.restActions.assignRole(guild, member, role);
        this.temporaryRoleService.setTemporaryRole(member.getUser(), role);
    }

    private Optional<BaymaxConfig.HelpDesk> findHelpDesk(long channelId) {
        return this.baymaxConfig.helpDesks().stream()
            .filter(helpDesk -> helpDesk.channelId() == channelId)
            .findAny();
    }

    private boolean isStaff(Member member) {
        return member.getRoles().stream()
            .anyMatch(role -> this.baymaxConfig.staffRoleIds().contains(role.getIdLong()));
    }
}
