package vip.mate.plugin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vip.mate.common.result.R;
import vip.mate.plugin.PluginManager;
import vip.mate.plugin.model.PluginInfo;
import vip.mate.workspace.core.annotation.RequireWorkspaceRole;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Plugin management REST API.
 *
 * @author Invt Team
 */
@Slf4j
@Tag(name = "Plugin Management")
@RestController
@RequestMapping("/api/v1/plugins")
@RequiredArgsConstructor
public class PluginController {

    private final PluginManager pluginManager;

    @Operation(summary = "List all plugins")
    @GetMapping
    @RequireWorkspaceRole("admin")
    public R<List<PluginInfo>> list() {
        return R.ok(pluginManager.listPlugins());
    }

    @Operation(summary = "Get plugin detail")
    @GetMapping("/{name}")
    @RequireWorkspaceRole("admin")
    public R<PluginInfo> get(@PathVariable String name) {
        return R.ok(pluginManager.getPlugin(name));
    }

    @Operation(summary = "Disable a plugin")
    @PostMapping("/{name}/disable")
    @RequireWorkspaceRole("admin")
    public R<Void> disable(@PathVariable String name) {
        pluginManager.disablePlugin(name);
        return R.ok();
    }

    @Operation(summary = "Enable a plugin")
    @PostMapping("/{name}/enable")
    @RequireWorkspaceRole("admin")
    public R<Void> enable(@PathVariable String name) {
        pluginManager.enablePlugin(name);
        return R.ok();
    }

    @Operation(summary = "Update plugin configuration")
    @PutMapping("/{name}/config")
    @RequireWorkspaceRole("admin")
    public R<Void> updateConfig(@PathVariable String name,
                                @RequestBody Map<String, Object> config) {
        pluginManager.updateConfig(name, config);
        return R.ok();
    }

    @Operation(summary = "Upload and import a plugin JAR file")
    @PostMapping("/upload")
    @RequireWorkspaceRole("admin")
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("请选择要上传的 JAR 文件");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".jar")) {
            return R.fail("仅支持上传 .jar 文件");
        }
        try {
            // Save to user-global plugin directory
            Path pluginDir = Paths.get(System.getProperty("user.home"), ".invt", "plugins");
            Files.createDirectories(pluginDir);
            Path target = pluginDir.resolve(originalName);
            file.transferTo(target.toFile());
            log.info("Plugin JAR uploaded: {}", target.toAbsolutePath());
            // Trigger plugin reload
            try {
                pluginManager.loadAllPlugins();
            } catch (Exception e) {
                log.warn("Plugin auto-reload after upload failed (may need manual refresh): {}", e.getMessage());
            }
            return R.ok(Map.of("fileName", originalName, "path", target.toAbsolutePath().toString()));
        } catch (IOException e) {
            log.error("Failed to save plugin JAR: {}", e.getMessage());
            return R.fail("文件保存失败: " + e.getMessage());
        }
    }
}
