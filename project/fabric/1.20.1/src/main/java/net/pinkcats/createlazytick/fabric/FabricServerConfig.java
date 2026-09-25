package net.pinkcats.createlazytick.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Fabric-only configuration for migrated optimizations. */
public final class FabricServerConfig {
    private static volatile boolean enableStalledPassengers = false;
    private static volatile int stalledPassengerInterval = 5;

    private FabricServerConfig() {}

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("createlazytick-fabric.properties");
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (InputStream input = Files.newInputStream(path)) {
                properties.load(input);
            } catch (IOException error) {
                throw new IllegalStateException("Cannot read CreateLazyTick Fabric config: " + path, error);
            }
        }
        enableStalledPassengers = Boolean.parseBoolean(properties.getProperty("enable_lazy_belt_stalled_passengers", "false"));
        try {
            stalledPassengerInterval = Math.max(2, Math.min(20, Integer.parseInt(properties.getProperty("belt_stalled_passenger_interval", "5"))));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Invalid belt_stalled_passenger_interval in " + path, error);
        }
        if (!Files.exists(path)) {
            properties.setProperty("enable_lazy_belt_stalled_passengers", Boolean.toString(enableStalledPassengers));
            properties.setProperty("belt_stalled_passenger_interval", Integer.toString(stalledPassengerInterval));
            try {
                Files.createDirectories(path.getParent());
                try (OutputStream output = Files.newOutputStream(path)) {
                    properties.store(output, "CreateLazyTick Fabric 1.20.1 options; restart to apply changes");
                }
            } catch (IOException error) {
                throw new IllegalStateException("Cannot write CreateLazyTick Fabric config: " + path, error);
            }
        }
    }

    public static boolean enableStalledPassengers() { return enableStalledPassengers; }
    public static int stalledPassengerInterval() { return stalledPassengerInterval; }
}
