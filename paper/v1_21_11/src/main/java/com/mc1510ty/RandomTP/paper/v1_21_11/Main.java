package com.mc1510ty.RandomTP.paper.v1_21_11;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class Main extends JavaPlugin {

    private int radius;
    private String done_teleport;
    private int cooldown;
    private long cooldownMillis;
    private String worldname;
    private String cooldownmsg;
    private int RESISTANCE_time;
    private int move_y;
    private final Map<UUID, Long> lastRTP = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {

        saveDefaultConfig();

        radius = getConfig().getInt("radius", -1);
        done_teleport = getConfig().getString("done-teleport", null);
        cooldown = getConfig().getInt("cooldown", -1);
        worldname = getConfig().getString("worldname", null);
        cooldownmsg = getConfig().getString("cooldownmsg", null);
        RESISTANCE_time = getConfig().getInt("RESISTANCE-time", -1);
        move_y = getConfig().getInt("move-y", -1);

        if (radius == -1) {
            getLogger().info("Radius not set, using defaults.");
            radius = 100000;
        }
        if (done_teleport == null) {
            getLogger().info("done-teleport not set, using defaults.");
            done_teleport = "Random teleportation complete!";
        }
        if (cooldown == -1) {
            getLogger().info("cooldown not set, using defaults.");
            cooldown = 5;
        }
        if (worldname == null) {
            getLogger().info("worldname not set, using defaults.");
            worldname = "world";
        }
        if (cooldownmsg == null) {
            getLogger().info("cooldown not set, using defaults.");
            cooldownmsg = "Teleport is on cooldown. {seconds} seconds remaining.";
        }
        if (RESISTANCE_time == -1) {
            getLogger().info("RESISTANCE_time not set, using defaults.");
            RESISTANCE_time = 15;
        }
        if (move_y == -1) {
            getLogger().info("move_y not set, using defaults.");
            move_y = 321;
        }

        cooldownMillis = cooldown * 1000L;

        LiteralArgumentBuilder<CommandSourceStack> rtp = Commands.literal("rtp")
                .executes(context -> {
                    var sender = context.getSource().getSender();
                    Player player = (Player) sender;
                    var world = Bukkit.getWorld(worldname);

                    double theta = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
                    double r = Math.sqrt(ThreadLocalRandom.current().nextDouble()) * radius;

                    int x = (int) Math.round(Math.cos(theta) * r);
                    int z = (int) Math.round(Math.sin(theta) * r);

                    lastRTP.compute(player.getUniqueId(), (uuid, last) -> {
                        long now = System.currentTimeMillis();
                        if (last != null) {
                            long remaining = cooldownMillis - (now - last);
                            if (remaining > 0) {
                                long secondsLeft = (remaining + 999) / 1000; // 切り上げ
                                String msg = cooldownmsg.replace("{seconds}", String.valueOf(secondsLeft));
                                player.sendMessage(Component.text(msg));
                                return last;
                            }
                        }

                        player.getScheduler().execute(this, () -> {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, RESISTANCE_time * 20, 255, false, false, false));
                            player.sendMessage(Component.text(done_teleport, NamedTextColor.GREEN));
                            player.teleportAsync(new Location(world, x, move_y, z));
                        },null,0);

                        return now;
                    });

                    return 1;
                });
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> commands.registrar().register(rtp.build()));
    }
}