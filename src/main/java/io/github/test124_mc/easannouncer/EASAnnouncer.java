package io.github.test124_mc.easannouncer;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Particle;
import org.bukkit.Color;
import org.bukkit.Particle.DustOptions;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class EASAnnouncer extends JavaPlugin {

    // Configuration keys
    private static final String CONFIG_HEADER_KEY = "messages.header";
    private static final String CONFIG_FOOTER_KEY = "messages.footer";
    private static final String CONFIG_SOUND_KEY = "sound.name";
    private static final String CONFIG_VOLUME_KEY = "sound.volume";
    private static final String CONFIG_PITCH_KEY = "sound.pitch";
    private static final String CONFIG_SHAKE_DURATION_KEY = "earthquake.shake_duration_ticks";
    private static final String CONFIG_SHAKE_INTENSITY_KEY = "earthquake.shake_intensity";

    private static final String CONFIG_TSUNAMI_PARTICLE_COUNT_KEY = "tsunami.particle_count";
    private static final String CONFIG_TSUNAMI_PARTICLE_SPREAD_RADIUS_KEY = "tsunami.particle_spread_radius";
    private static final String CONFIG_TSUNAMI_PARTICLE_COLOR_RED_KEY = "tsunami.particle_color.red";
    private static final String CONFIG_TSUNAMI_PARTICLE_COLOR_GREEN_KEY = "tsunami.particle_color.green";
    private static final String CONFIG_TSUNAMI_PARTICLE_COLOR_BLUE_KEY = "tsunami.particle_color.blue";
    private static final String CONFIG_TSUNAMI_SOUND_KEY = "tsunami.sound";

    private static final String CONFIG_TITLE_FADE_IN_KEY = "title_display.fade_in_ticks";
    private static final String CONFIG_TITLE_STAY_KEY = "title_display.stay_ticks";
    private static final String CONFIG_TITLE_FADE_OUT_KEY = "title_display.fade_out_ticks";


    private String easHeader;
    private String easFooter;
    private Sound easSound;
    private float soundVolume;
    private float soundPitch;
    private int earthquakeShakeDuration;
    private double earthquakeShakeIntensity;

    private int tsunamiParticleCount;
    private double tsunamiParticleSpreadRadius;
    private Color tsunamiParticleColor;
    private Sound tsunamiSound;

    private int titleFadeInTicks;
    private int titleStayTicks;
    private int titleFadeOutTicks;


    private MiniMessage miniMessage;

    @Override
    public void onEnable() {
        getLogger().info("EASAnnouncer plugin has been enabled!");

        miniMessage = MiniMessage.miniMessage();

        saveDefaultConfig();
        loadConfigValues();

        getCommand("easbroadcast").setExecutor(this);
        getCommand("easreload").setExecutor(this);
        getCommand("easearthquake").setExecutor(this);
        getCommand("eastsunami").setExecutor(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("EASAnnouncer plugin has been disabled!");
    }

    private void loadConfigValues() {
        this.easHeader = getConfig().getString(CONFIG_HEADER_KEY, "<red><bold>*** EMERGENCY ALERT SYSTEM ***</bold></red>");
        this.easFooter = getConfig().getString(CONFIG_FOOTER_KEY, "<red><bold>*** THIS IS ONLY A TEST ***</bold></red>");

        String soundName = getConfig().getString(CONFIG_SOUND_KEY, "BLOCK_RESPAWN_ANCHOR_CHARGE");
        try {
            this.easSound = Sound.valueOf(soundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid EAS sound name in config.yml: " + soundName + ". Using default sound.");
            this.easSound = Sound.BLOCK_RESPAWN_ANCHOR_CHARGE;
        }
        this.soundVolume = (float) getConfig().getDouble(CONFIG_VOLUME_KEY, 1.0);
        this.soundPitch = (float) getConfig().getDouble(CONFIG_PITCH_KEY, 0.5);

        this.earthquakeShakeDuration = getConfig().getInt(CONFIG_SHAKE_DURATION_KEY, 60);
        this.earthquakeShakeIntensity = getConfig().getDouble(CONFIG_SHAKE_INTENSITY_KEY, 0.2);

        this.tsunamiParticleCount = getConfig().getInt(CONFIG_TSUNAMI_PARTICLE_COUNT_KEY, 150);
        this.tsunamiParticleSpreadRadius = getConfig().getDouble(CONFIG_TSUNAMI_PARTICLE_SPREAD_RADIUS_KEY, 3.0);
        int red = getConfig().getInt(CONFIG_TSUNAMI_PARTICLE_COLOR_RED_KEY, 0);
        int green = getConfig().getInt(CONFIG_TSUNAMI_PARTICLE_COLOR_GREEN_KEY, 0);
        int blue = getConfig().getInt(CONFIG_TSUNAMI_PARTICLE_COLOR_BLUE_KEY, 255);
        this.tsunamiParticleColor = Color.fromRGB(red, green, blue);

        String tsunamiSoundName = getConfig().getString(CONFIG_TSUNAMI_SOUND_KEY, "ENTITY_ENDERMAN_DEATH");
        try {
            this.tsunamiSound = Sound.valueOf(tsunamiSoundName.toUpperCase());
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid Tsunami sound name in config.yml: " + tsunamiSoundName + ". Using default sound.");
            this.tsunamiSound = Sound.ENTITY_ENDERMAN_DEATH;
        }

        this.titleFadeInTicks = getConfig().getInt(CONFIG_TITLE_FADE_IN_KEY, 10);
        this.titleStayTicks = getConfig().getInt(CONFIG_TITLE_STAY_KEY, 70);
        this.titleFadeOutTicks = getConfig().getInt(CONFIG_TITLE_FADE_OUT_KEY, 20);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("easbroadcast")) {
            if (!sender.hasPermission("easannouncer.broadcast")) {
                sender.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(Component.text("Usage: /easbroadcast <message>").color(NamedTextColor.YELLOW));
                return true;
            }
            String rawMessage = String.join(" ", args);
            broadcastEAS(easHeader, easSound, soundVolume, soundPitch, false, false, rawMessage);
            getLogger().info("EAS broadcast triggered by " + sender.getName() + ": " + rawMessage);
            return true;
        } else if (command.getName().equalsIgnoreCase("easearthquake")) {
            if (!sender.hasPermission("easannouncer.earthquake")) {
                sender.sendMessage(Component.text("You don't have permission to trigger an earthquake.").color(NamedTextColor.RED));
                return true;
            }
            String message = (args.length > 0) ? String.join(" ", args) : "An earthquake has been detected!";
            broadcastEAS("<yellow><bold>EARTHQUAKE ALERT!</bold></yellow>", easSound, soundVolume, soundPitch, true, false, message);
            getLogger().info("Earthquake EAS triggered by " + sender.getName() + ": " + message);
            return true;
        } else if (command.getName().equalsIgnoreCase("eastsunami")) {
            if (!sender.hasPermission("easannouncer.tsunami")) {
                sender.sendMessage(Component.text("You don't have permission to trigger a tsunami alert.").color(NamedTextColor.RED));
                return true;
            }
            String message = (args.length > 0) ? String.join(" ", args) : "Tsunami warning issued for coastal areas!";
            broadcastEAS("<aqua><bold>TSUNAMI WARNING!</bold></aqua>", easSound, soundVolume, soundPitch, false, true, message);
            getLogger().info("Tsunami EAS triggered by " + sender.getName() + ": " + message);
            return true;
        } else if (command.getName().equalsIgnoreCase("easreload")) {
            if (!sender.hasPermission("easannouncer.reload")) {
                sender.sendMessage(Component.text("You don't have permission to reload the config.").color(NamedTextColor.RED));
                return true;
            }
            reloadConfig();
            loadConfigValues();
            sender.sendMessage(Component.text("EASAnnouncer config reloaded!").color(NamedTextColor.GREEN));
            getLogger().info("EASAnnouncer config reloaded by " + sender.getName());
            return true;
        }
        return false;
    }

    private void broadcastEAS(String titleRaw, Sound sound, float volume, float pitch, boolean screenShake, boolean tsunamiEffect, String subtitleRaw) {
        Title title = Title.title(
                miniMessage.deserialize(titleRaw),
                miniMessage.deserialize(subtitleRaw),
                // CORRECTED: Using Duration.ofMillis() for Java 8 compatibility
                // Convert ticks to milliseconds (1 tick = 50 ms)
                Title.Times.times(
                        Duration.ofMillis((long) (titleFadeInTicks * 50.0)),
                        Duration.ofMillis((long) (titleStayTicks * 50.0)),
                        Duration.ofMillis((long) (titleFadeOutTicks * 50.0))
                )
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
            player.playSound(player.getLocation(), sound, volume, pitch);

            if (screenShake) {
                applyScreenShake(player);
            }
            if (tsunamiEffect) {
                applyTsunamiParticlesAndSound(player);
            }
        }
    }

    private void applyScreenShake(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            final double intensity = earthquakeShakeIntensity;
            final Random random = ThreadLocalRandom.current();

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= earthquakeShakeDuration) {
                    this.cancel();
                    return;
                }

                Location loc = player.getLocation();
                float newYaw = loc.getYaw() + (float) ((random.nextDouble() - 0.5) * 2 * intensity * 10);
                float newPitch = loc.getPitch() + (float) ((random.nextDouble() - 0.5) * 2 * intensity * 10);

                newPitch = Math.max(-90, Math.min(90, newPitch));

                player.teleport(new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ(), newYaw, newPitch));
                ticks++;
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private void applyTsunamiParticlesAndSound(Player player) {
        player.getWorld().playSound(player.getLocation(), tsunamiSound, 1.5f, 1.0f);

        DustOptions dustOptions = new DustOptions(tsunamiParticleColor, 1.0f);

        for (int i = 0; i < tsunamiParticleCount; i++) {
            double offsetX = ThreadLocalRandom.current().nextDouble(-tsunamiParticleSpreadRadius, tsunamiParticleSpreadRadius);
            double offsetY = ThreadLocalRandom.current().nextDouble(-tsunamiParticleSpreadRadius, tsunamiParticleSpreadRadius);
            double offsetZ = ThreadLocalRandom.current().nextDouble(-tsunamiParticleSpreadRadius, tsunamiParticleSpreadRadius);

            player.getWorld().spawnParticle(
                    Particle.DUST,
                    player.getLocation().add(offsetX, offsetY, offsetZ),
                    1,
                    0, 0, 0,
                    0,
                    dustOptions
            );
        }
        player.sendMessage(miniMessage.deserialize("<dark_gray><italic>A chilling sound echoes from the depths, and strange particles appear...</italic></dark_gray>"));
    }

    /*
    private void checkExternalAPIForEarthquakes() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=NOW-1hour&minmagnitude=4");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        String inputLine;
                        StringBuilder content = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            content.append(inputLine);
                        }
                        in.close();
                        connection.disconnect();

                        // Parse the JSON response
                        // For demonstration, let's assume we detected a significant earthquake:
                        // if (detected_significant_earthquake_based_on_magnitude_and_proximity_to_server_location) {
                        //     broadcastEAS("<yellow><bold>EARTHQUAKE ALERT!</bold></yellow>", easSound, soundVolume, soundPitch, true, false, "Magnitude " + magnitude + " detected near " + location_name + "!");
                        // }

                    } else {
                        getLogger().warning("Failed to get earthquake data from API. Response Code: " + responseCode);
                    }
                } catch (Exception e) {
                    getLogger().severe("Error checking earthquake API: " + e.getMessage());
                }
            }
        }.runTaskTimerAsynchronously(this, 0L, 20L * 60 * 5);
    }
    */
}