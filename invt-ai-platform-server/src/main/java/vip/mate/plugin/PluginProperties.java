package vip.mate.plugin;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Plugin SDK configuration properties.
 *
 * @author Invt Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "invt.plugin")
public class PluginProperties {

    /** Whether the plugin system is enabled */
    private boolean enabled = true;

    /** User-global plugin directory */
    private String userDir = System.getProperty("user.home") + "/.invt/plugins";
}
