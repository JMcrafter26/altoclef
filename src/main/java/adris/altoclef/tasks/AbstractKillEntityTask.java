package adris.altoclef.tasks;

import adris.altoclef.AltoClef;
import adris.altoclef.tasksystem.Task;
import adris.altoclef.util.EntityUtil;
import baritone.api.pathing.goals.GoalRunAway;
import baritone.api.utils.RayTraceUtils;
import baritone.api.utils.Rotation;
import baritone.api.utils.RotationUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public abstract class AbstractKillEntityTask extends Task {

    private static final double OTHER_FORCE_FIELD_RANGE = 2;

    // Not the "striking" distance, but the "ok we're close enough, lower our guard for other mobs and focus on this one" range.
    private static final double CONSIDER_COMBAT_RANGE = 10;

    private static final Item[] WEAPON_ITEMS = new Item[] {
            Items.DIAMOND_SWORD,
            Items.IRON_SWORD,
            Items.STONE_SWORD,
            Items.WOODEN_SWORD,
            Items.GOLDEN_SWORD,
            Items.DIAMOND_AXE,
            Items.IRON_AXE,
            Items.STONE_AXE,
            Items.WOODEN_AXE,
            Items.GOLDEN_AXE
    };

    private static final double MAINTAIN_DISTANCE = 3;

    @Override
    protected void onStart(AltoClef mod) {
    }

    @Override
    protected Task onTick(AltoClef mod) {
        LivingEntity entity = getEntityTarget(mod);

        mod.getMobDefenseChain().setTargetEntity(entity);

        // Oof
        if (entity == null) {
            mod.getMobDefenseChain().resetForceField();
            return null;
        }

        double playerReach = mod.getClientBaritone().getPlayerContext().playerController().getBlockReachDistance();

        EntityHitResult result = EntityUtil.raycast(mod.getPlayer(), entity, playerReach);

        float hitProg = mod.getPlayer().getAttackCooldownProgress(0);

        double sqDist = entity.squaredDistanceTo(mod.getPlayer());

        if (sqDist < CONSIDER_COMBAT_RANGE*CONSIDER_COMBAT_RANGE) {
            mod.getMobDefenseChain().setForceFieldRange(OTHER_FORCE_FIELD_RANGE);
        } else {
            mod.getMobDefenseChain().resetForceField();
        }

        boolean tooClose = sqDist < MAINTAIN_DISTANCE*MAINTAIN_DISTANCE;
        // Step away if we're too close
        if (tooClose) {
            setDebugState("Maintaining distance");
            if (!mod.getClientBaritone().getCustomGoalProcess().isActive()) {
                mod.getClientBaritone().getCustomGoalProcess().setGoalAndPath(new GoalRunAway(MAINTAIN_DISTANCE, entity.getBlockPos()));
            }
        }

        if (entity.squaredDistanceTo(mod.getPlayer()) < playerReach*playerReach && result != null && result.getType() == HitResult.Type.ENTITY) {
            // Equip weapon
            equipWeapon(mod);
            if (hitProg >= 0.99) {
                mod.getController().attackEntity(mod.getPlayer(), entity);
            }
        } else if (!tooClose) {
            setDebugState("Approaching target");
            // Move to target

            return new GetToEntityTask(entity);
        }

        return null;
    }

    @Override
    protected void onStop(AltoClef mod, Task interruptTask) {
        mod.getMobDefenseChain().setTargetEntity(null);
        mod.getMobDefenseChain().resetForceField();
    }

    protected abstract LivingEntity getEntityTarget(AltoClef mod);

    private void equipWeapon(AltoClef mod) {
        for (Item item : WEAPON_ITEMS) {
            if (mod.getInventoryTracker().hasItem(item)) {
                mod.getInventoryTracker().equipItem(item);
                return;
            }
        }
    }
}