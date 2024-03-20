/*
    Copyright(c) 2021 AuroraLS3

    The MIT License(MIT)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files(the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions :
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package net.playeranalytics.extension.libertybans;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.FormatType;
import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.extension.annotation.DataBuilderProvider;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.builder.ExtensionDataBuilder;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.query.QueryService;
import space.arim.libertybans.api.*;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.select.SelectionPredicate;
import space.arim.libertybans.api.select.SortPunishments;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

import java.util.Optional;
import java.util.UUID;

/**
 * LibertyBans DataExtension.
 *
 * @author Vankka
 */
@PluginInfo(name = "LibertyBans", iconName = "gavel", iconFamily = Family.SOLID, color = Color.RED)
public class LibertyBansExtension implements DataExtension {

    private LibertyBans api;

    public LibertyBansExtension() {
    }

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.PLAYER_JOIN, CallEvents.PLAYER_LEAVE
        };
    }

    private LibertyBans api() {
        if (api != null) {
            return api;
        }

        Omnibus omnibus = OmnibusProvider.getOmnibus();
        this.api = omnibus.getRegistry().getProvider(LibertyBans.class).orElseThrow(NotReadyException::new);
        if (api == null) throw new NotReadyException();
        return api;
    }

    private Optional<Punishment> punishment(UUID playerUUID, PunishmentType type) {
        return api()
                .getSelector()
                .selectionBuilder()
                .victims(
                        SelectionPredicate.matchingAnyOf(
                                PlayerVictim.of(playerUUID),
                                CompositeVictim.of(playerUUID, CompositeVictim.WILDCARD_ADDRESS)
                        )
                )
                .type(type)
                .build()
                .getFirstSpecificPunishment(SortPunishments.LATEST_END_DATE_FIRST)
                .toCompletableFuture().join(); // We're off-thread, we just block for the information
    }

    private String prettyOperator(Operator operator) {
        if (operator instanceof ConsoleOperator) {
            return "CONSOLE";
        } else if (operator instanceof PlayerOperator) {
            UUID uuid = ((PlayerOperator) operator).getUUID();
            return QueryService.getInstance().getCommonQueries()
                    .fetchNameOf(uuid).orElse("Unknown");
        } else {
            return operator.getType().name();
        }
    }

    @DataBuilderProvider
    public ExtensionDataBuilder punishmentData(UUID playerUUID) {
        Optional<Punishment> possibleBan = punishment(playerUUID, PunishmentType.BAN);
        Optional<Punishment> possibleMute = punishment(playerUUID, PunishmentType.MUTE);

        ExtensionDataBuilder builder = newExtensionDataBuilder()
                .addValue(Boolean.class, valueBuilder("Banned")
                        .description("Is the player banned on LibertyBans")
                        .priority(100)
                        .icon(Icon.called("gavel").of(Color.RED).build())
                        .buildBoolean(possibleBan.isPresent()))
                .addValue(Boolean.class, valueBuilder("Muted")
                        .description("Is the player muted on LibertyBans")
                        .priority(50)
                        .icon(Icon.called("bell-slash").of(Color.DEEP_ORANGE).build())
                        .buildBoolean(possibleMute.isPresent()));

        possibleBan.ifPresent(ban -> builder
                .addValue(String.class, valueBuilder("Banned by")
                        .description("Who banned the player")
                        .priority(99)
                        .icon(Icon.called("user").of(Color.RED).build())
                        .showAsPlayerPageLink()
                        .buildString(prettyOperator(ban.getOperator())))
                .addValue(Long.class, () -> {
                    try {
                        return valueBuilder("Date")
                                .description("When the ban was issued")
                                .priority(98)
                                .icon(Icon.called("calendar").of(Color.RED).of(Family.REGULAR).build())
                                .format(FormatType.DATE_YEAR)
                                .buildNumber(ban.getStartDate().toEpochMilli());
                    } catch (ArithmeticException outOfBounds) {
                        return null;
                    }
                })
                .addValue(Long.class, () -> {
                    try {
                        return valueBuilder("Ends")
                                .description("When the ban expires")
                                .priority(96)
                                .icon(Icon.called("calendar-check").of(Color.RED).of(Family.REGULAR).build())
                                .format(FormatType.DATE_YEAR)
                                .buildNumber(ban.getEndDate().toEpochMilli());
                    } catch (ArithmeticException outOfBounds) {
                        return null;
                    }
                })
                .addValue(String.class, valueBuilder("Reason")
                        .description("Why the ban was issued")
                        .priority(95)
                        .icon(Icon.called("comment").of(Family.REGULAR).of(Color.RED).build())
                        .buildString(ban.getReason()))
        );

        possibleMute.ifPresent(mute -> builder
                .addValue(String.class, valueBuilder("Muted by")
                        .description("Who banned the player")
                        .priority(49)
                        .icon(Icon.called("user").of(Color.DEEP_ORANGE).build())
                        .showAsPlayerPageLink()
                        .buildString(prettyOperator(mute.getOperator())))
                .addValue(Long.class, valueBuilder("Date")
                        .description("When the mute was issued")
                        .priority(48)
                        .icon(Icon.called("calendar").of(Color.DEEP_ORANGE).of(Family.REGULAR).build())
                        .format(FormatType.DATE_YEAR)
                        .buildNumber(mute.getStartDate().toEpochMilli()))
                .addValue(Long.class, valueBuilder("Ends")
                        .description("When the mute expires")
                        .priority(46)
                        .icon(Icon.called("calendar-check").of(Color.DEEP_ORANGE).of(Family.REGULAR).build())
                        .format(FormatType.DATE_YEAR)
                        .buildNumber(mute.getEndDate().toEpochMilli()))
                .addValue(String.class, valueBuilder("Reason")
                        .description("Why the mute was issued")
                        .priority(45)
                        .icon(Icon.called("comment").of(Family.REGULAR).of(Color.DEEP_ORANGE).build())
                        .buildString(mute.getReason()))
        );

        return builder;
    }
}
