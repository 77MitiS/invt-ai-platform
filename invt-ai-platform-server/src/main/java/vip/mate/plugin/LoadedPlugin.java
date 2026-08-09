package vip.mate.plugin;

import lombok.Data;
import vip.mate.plugin.api.InvtPlugin;
import vip.mate.plugin.api.PluginManifest;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal state holder for a loaded plugin.
 *
 * @author Invt Team
 */
@Data
public class LoadedPlugin {

    private final PluginManifest manifest;
    private final InvtPlugin plugin;
    private final URLClassLoader classLoader;

    /** Set after construction (circular reference with PluginContextImpl) */
    private PluginContextImpl context;

    /** Names of tools registered by this plugin */
    private final List<String> registeredTools = new ArrayList<>();

    /** Channel types registered by this plugin */
    private final List<String> registeredChannels = new ArrayList<>();

    /** Provider ID registered by this plugin (null if none) */
    private String registeredProvider;

    /** Memory provider ID registered by this plugin (null if none) */
    private String registeredMemoryProvider;

    /** Whether the plugin is currently enabled */
    private boolean enabled = true;

    public LoadedPlugin(PluginManifest manifest, InvtPlugin plugin,
                        URLClassLoader classLoader) {
        this.manifest = manifest;
        this.plugin = plugin;
        this.classLoader = classLoader;
    }
}
