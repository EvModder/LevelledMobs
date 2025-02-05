package me.lokka30.levelledmobs.listeners.paper;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.LivingEntityWrapper;
import me.lokka30.levelledmobs.result.NametagResult;
import me.lokka30.levelledmobs.util.Utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerDeathListener {

    public PlayerDeathListener(final LevelledMobs main) {
        this.main = main;
    }

    private final LevelledMobs main;
    private boolean shouldCancelEvent;

    public boolean onPlayerDeathEvent(final @NotNull PlayerDeathEvent event) {
        this.shouldCancelEvent = false;
        if (event.deathMessage() == null) {
            return true;
        }
        if (!(event.deathMessage() instanceof TranslatableComponent)) {
            return false;
        }

        final LivingEntityWrapper lmEntity = getPlayersKiller(event);

        if (lmEntity == null) {
            if (main.placeholderApiIntegration != null) {
                main.placeholderApiIntegration.putPlayerOrMobDeath(event.getEntity(), null, true);
            }
            if (this.shouldCancelEvent) event.setCancelled(true);
            return true;
        }

        if (main.placeholderApiIntegration != null) {
            main.placeholderApiIntegration.putPlayerOrMobDeath(event.getEntity(), lmEntity, true);
        }
        lmEntity.free();

        if (this.shouldCancelEvent) event.setCancelled(true);
        return true;
    }

    @Nullable private LivingEntityWrapper getPlayersKiller(@NotNull final PlayerDeathEvent event) {
        final EntityDamageEvent entityDamageEvent = event.getEntity().getLastDamageCause();
        if (entityDamageEvent == null || entityDamageEvent.isCancelled()
            || !(entityDamageEvent instanceof EntityDamageByEntityEvent)) {
            return null;
        }

        final Entity damager = ((EntityDamageByEntityEvent) entityDamageEvent).getDamager();
        LivingEntity killer = null;

        if (damager instanceof final Projectile projectile) {
            if (projectile.getShooter() instanceof LivingEntity) {
                killer = (LivingEntity) projectile.getShooter();
            }
        } else if (damager instanceof LivingEntity) {
            killer = (LivingEntity) damager;
        }

        if (killer == null || Utils.isNullOrEmpty(killer.getName()) || killer instanceof Player) {
            return null;
        }

        final LivingEntityWrapper lmKiller = LivingEntityWrapper.getInstance(killer, main);
        if (!lmKiller.isLevelled()) {
            return lmKiller;
        }

        final NametagResult mobNametag = main.levelManager.getNametag(lmKiller, true, true);
        if (mobNametag.getNametag() != null && mobNametag.getNametag().isEmpty()){
            this.shouldCancelEvent = true;
            return lmKiller;
        }

        if (mobNametag.isNullOrEmpty() || "disabled".equalsIgnoreCase(mobNametag.getNametag())) {
            return lmKiller;
        }

        updateDeathMessage(event, mobNametag);

        return lmKiller;
    }

    private void updateDeathMessage(final @NotNull PlayerDeathEvent event, final @NotNull NametagResult nametagResult) {
        if (!(event.deathMessage() instanceof final TranslatableComponent tc)) {
            return;
        }

        final String playerKilled = extractPlayerName(tc);
        if (playerKilled == null) {
            return;
        }

        String mobKey = null;
        Component itemComp = null;
        for (final Component c : tc.args()){
            if (c instanceof final TranslatableComponent tc2) {
                if ("chat.square_brackets".equals(tc2.key())) {
                    // this is when the mob was holding a weapon
                    itemComp = tc2;
                }
                else {
                    mobKey = tc2.key();
                }
            }
        }

        if (mobKey == null) {
            return;
        }
        final String mobName = nametagResult.getNametagNonNull();

        Component newCom;
        if (nametagResult.hadCustomDeathMessage){
            newCom = LegacyComponentSerializer.legacyAmpersand().deserialize(
                    mobName.replace("%player%", playerKilled));
        }
        else {
            final int displayNameIndex = mobName.indexOf("{DisplayName}");
            final Component leftComp = displayNameIndex > 0 ?
                    LegacyComponentSerializer.legacyAmpersand().deserialize(mobName.substring(0, displayNameIndex)) :
                    Component.empty();
            final Component rightComp = mobName.length() > displayNameIndex + 13 ?
                    LegacyComponentSerializer.legacyAmpersand().deserialize(mobName.substring(displayNameIndex + 13)) :
                    Component.empty();

            final Component mobNameComponent = nametagResult.overriddenName == null ?
                    Component.translatable(mobKey) :
                    LegacyComponentSerializer.legacyAmpersand().deserialize(nametagResult.overriddenName);

            if (itemComp == null) {
                // mob wasn't using any weapon
                // 2 arguments, example: "death.attack.mob": "%1$s was slain by %2$s"
                newCom = Component.translatable(tc.key(),
                        Component.text(playerKilled),
                        leftComp.append(mobNameComponent)
                ).append(rightComp);
            }
            else {
                // mob had a weapon and it's details are stored in the itemComp component
                // 3 arguments, example: "death.attack.mob.item": "%1$s was slain by %2$s using %3$s"
                newCom = Component.translatable(tc.key(),
                        Component.text(playerKilled),
                        leftComp.append(mobNameComponent),
                        itemComp
                ).append(rightComp);
            }
        }

        event.deathMessage(newCom);
    }

    @Nullable private String extractPlayerName(final @NotNull TranslatableComponent tc) {
        String playerKilled = null;

        for (final Component com : tc.args()) {
            if (!(com instanceof final TextComponent tc2)) continue;
            playerKilled = tc2.content();

            if (playerKilled.isEmpty() && tc2.hoverEvent() == null) continue;

            // in rare cases the above method returns a empty string
            // we'll extract the player name from the hover event
            final HoverEvent<?> he = tc2.hoverEvent();
            if (he == null || !(he.value() instanceof final HoverEvent.ShowEntity se)) {
                return null;
            }

            if (se.name() instanceof final TextComponent tc3) {
                playerKilled = tc3.content();
            }
        }

        return playerKilled == null || playerKilled.isEmpty() ?
            null : playerKilled;
    }
}
