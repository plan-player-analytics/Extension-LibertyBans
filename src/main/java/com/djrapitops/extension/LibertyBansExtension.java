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
package com.djrapitops.extension;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.FormatType;
import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.extension.annotation.*;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.query.CommonQueries;
import com.djrapitops.plan.query.QueryService;
import space.arim.libertybans.api.*;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.libertybans.api.punish.PunishmentBase;
import space.arim.libertybans.api.select.PunishmentSelector;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

import java.time.Instant;
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

    public LibertyBansExtension() {}

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
        return api;
    }

    private PunishmentSelector selector() {
        return api().getSelector();
    }

    private Optional<Punishment> punishment(UUID playerUUID, PunishmentType type) {
        return selector().getApplicablePunishment(playerUUID, null, type)
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

    @BooleanProvider(
            text = "Banned",
            description = "Is the player banned on BanManager",
            priority = 100,
            conditionName = "banned",
            iconName = "gavel",
            iconColor = Color.RED
    )
    public boolean isBanned(UUID playerUUID) {
        return punishment(playerUUID, PunishmentType.BAN).isPresent();
    }

    @Conditional("banned")
    @StringProvider(
            text = "Banned by",
            description = "Who banned the player",
            priority = 99,
            iconName = "user",
            iconColor = Color.RED,
            playerName = true
    )
    public String banIssuer(UUID playerUUID) {
        return punishment(playerUUID, PunishmentType.BAN)
                .map(PunishmentBase::getOperator)
                .map(this::prettyOperator)
                .orElseThrow(IllegalStateException::new);
    }

    @Conditional("banned")
    @NumberProvider(
            text = "Date",
            description = "When the ban was issued",
            priority = 98,
            iconName = "calendar",
            iconFamily = Family.REGULAR,
            iconColor = Color.RED,
            format = FormatType.DATE_YEAR
    )
    public long banIssueDate(UUID playerUUID) {
        return punishment(playerUUID, PunishmentType.BAN)
                .map(Punishment::getStartDate)
                .map(Instant::toEpochMilli)
                .orElseThrow(IllegalStateException::new);
    }

    @Conditional("banned")
    @NumberProvider(
            text = "Ends",
            description = "When the ban expires",
            priority = 96,
            iconName = "calendar-check",
            iconFamily = Family.REGULAR,
            iconColor = Color.RED,
            format = FormatType.DATE_YEAR
    )
    public long banExpireDate(UUID playerUUID) {
        return punishment(playerUUID, PunishmentType.BAN)
                .map(Punishment::getEndDate)
                .map(Instant::toEpochMilli)
                .orElseThrow(IllegalStateException::new);
    }

    @Conditional("banned")
    @StringProvider(
            text = "Reason",
            description = "Why the ban was issued",
            priority = 95,
            iconName = "comment",
            iconFamily = Family.REGULAR,
            iconColor = Color.RED
    )
    public String banReason(UUID playerUUID) {
        return punishment(playerUUID, PunishmentType.BAN)
                .map(Punishment::getReason)
                .orElseThrow(IllegalStateException::new);
    }

    @BooleanProvider(
            text = "Muted",
            description = "Is the player muted on BanManager",
            priority = 50,
            conditionName = "muted",
            iconName = "bell-slash",
            iconColor = Color.DEEP_ORANGE
    )
    public boolean isMuted(UUID playerUUID) {
        return punishment(playerUUID, PunishmentType.MUTE).isPresent();
    }

    @Conditional("muted")
    @StringProvider(
            text = "Muted by",
            description = "Who muted the player",
            priority = 49,
            iconName = "user",
            iconColor = Color.DEEP_ORANGE,
            playerName = true
    )
    public String muteIssuer(UUID playerUUID) {
        return punishment(playerUUID, PunishmentType.MUTE)
                .map(PunishmentBase::getOperator)
                .map(this::prettyOperator)
                .orElseThrow(IllegalStateException::new);
    }

    @Conditional("muted")
    @NumberProvider(
            text = "Date",
            description = "When the mute was issued",
            priority = 48,
            iconName = "calendar",
            iconFamily = Family.REGULAR,
            iconColor = Color.DEEP_ORANGE,
            format = FormatType.DATE_YEAR
    )
    public long muteIssueDate(UUID playerUUID) {
        return punishment(playerUUID, PunishmentType.MUTE)
                .map(Punishment::getStartDate)
                .map(Instant::toEpochMilli)
                .orElseThrow(IllegalStateException::new);
    }

    @Conditional("muted")
    @NumberProvider(
            text = "Ends",
            description = "When the mute expires",
            priority = 46,
            iconName = "calendar-check",
            iconFamily = Family.REGULAR,
            iconColor = Color.DEEP_ORANGE,
            format = FormatType.DATE_YEAR
    )
    public long muteExpireDate(UUID playerUUID) {
        return punishment(playerUUID, PunishmentType.MUTE)
                .map(Punishment::getEndDate)
                .map(Instant::toEpochMilli)
                .orElseThrow(IllegalStateException::new);
    }

    @Conditional("muted")
    @StringProvider(
            text = "Reason",
            description = "Why the mute was issued",
            priority = 45,
            iconName = "comment",
            iconFamily = Family.REGULAR,
            iconColor = Color.DEEP_ORANGE
    )
    public String muteReason(UUID playerUUID) {
        return punishment(playerUUID, PunishmentType.MUTE)
                .map(Punishment::getReason)
                .orElseThrow(IllegalStateException::new);
    }
}
