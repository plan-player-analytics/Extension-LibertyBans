package net.playeranalytics.extension.libertybans;

import com.djrapitops.plan.extension.Caller;
import space.arim.libertybans.api.CompositeVictim;
import space.arim.libertybans.api.PlayerVictim;
import space.arim.libertybans.api.Victim;
import space.arim.libertybans.api.event.PostPardonEvent;
import space.arim.libertybans.api.event.PostPunishEvent;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;
import space.arim.omnibus.events.EventBus;
import space.arim.omnibus.events.ListenerPriorities;

public class LibertyBansListener {

    private final Caller caller;

    public LibertyBansListener(Caller caller) {
        this.caller = caller;
    }

    public void register() {
        Omnibus omnibus = OmnibusProvider.getOmnibus();
        EventBus eventBus = omnibus.getEventBus();
        eventBus.registerListener(PostPunishEvent.class, ListenerPriorities.NORMAL, this::onPostPunish);
        eventBus.registerListener(PostPardonEvent.class, ListenerPriorities.NORMAL, this::onPostPardon);
    }

    public void onPostPunish(PostPunishEvent event) {
        actOnPunishment(event.getPunishment());
    }

    public void onPostPardon(PostPardonEvent event) {
        actOnPunishment(event.getPunishment());
    }

    private void actOnPunishment(Punishment punishment) {
        Victim victim = punishment.getVictim();
        if (victim instanceof PlayerVictim) {
            caller.updatePlayerData(((PlayerVictim) victim).getUUID(), null);
        }   else if (victim instanceof CompositeVictim) {
            caller.updatePlayerData(((CompositeVictim) victim).getUUID(), null);
        }
    }
}
