package vip.mate.system.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vip.mate.config.DatabaseBootstrapRunner;
import vip.mate.exception.InvtException;
import vip.mate.llm.model.ProviderInfoDTO;
import vip.mate.llm.service.ModelConfigService;
import vip.mate.llm.service.ModelProviderService;
import vip.mate.tool.browser.BrowserDiagnosticsService;
import vip.mate.tool.mcp.model.McpServerEntity;
import vip.mate.tool.mcp.runtime.McpClientManager;
import vip.mate.tool.mcp.runtime.McpClientManager.ConnectionResult;
import vip.mate.tool.mcp.service.McpServerService;

import java.util.ArrayList;
import java.util.List;

/**
 * System health check service.
 * <p>
 * Inspects default model, provider configurations, MCP server connections,
 * and database initialization status.
 *
 * @author Invt Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemHealthService {

    private final ModelConfigService modelConfigService;
    private final ModelProviderService modelProviderService;
    private final McpClientManager mcpClientManager;
    private final McpServerService mcpServerService;
    private final DatabaseBootstrapRunner bootstrapRunner;
    private final BrowserDiagnosticsService browserDiagnostics;

    public HealthResponse check() {
        List<HealthCheck> checks = new ArrayList<>();

        // 1. Default model check
        checks.add(checkDefaultModel());

        // 2. Provider checks (only providers that require API keys)
        checks.addAll(checkProviders());

        // 3. MCP server checks (enabled servers only)
        checks.addAll(checkMcpServers());

        // 4. Database initialization check
        checks.add(checkDatabase());

        // 5. Browser launch pre-flight (common failure source on fresh win/linux hosts)
        checks.add(checkBrowser());

        // Determine overall status
        String overall = "healthy";
        for (HealthCheck c : checks) {
            if ("error".equals(c.status())) {
                overall = "error";
                break;
            }
            if ("warning".equals(c.status())) {
                overall = "warning";
            }
        }

        return new HealthResponse(overall, checks);
    }

    private HealthCheck checkDefaultModel() {
        try {
            var model = modelConfigService.getDefaultModel();
            return new HealthCheck(
                    "默认模型",
                    "healthy",
                    "默认模型: " + model.getName(),
                    null
            );
        } catch (InvtException e) {
            return new HealthCheck(
                    "默认模型",
                    "error",
                    e.getMessage(),
                    new HealthAction("配置模型", "/settings/models")
            );
        }
    }

    private List<HealthCheck> checkProviders() {
        List<HealthCheck> results = new ArrayList<>();
        try {
            List<ProviderInfoDTO> providers = modelProviderService.listProviders();
            for (ProviderInfoDTO provider : providers) {
                // Only check providers that require an API key
                if (!Boolean.TRUE.equals(provider.getRequireApiKey())) {
                    continue;
                }
                String providerId = provider.getId();
                boolean configured = modelProviderService.isProviderConfigured(providerId);
                if (!configured) {
                    String reason = modelProviderService.getProviderUnavailableReason(providerId);
                    results.add(new HealthCheck(
                            "provider:" + providerId,
                            "warning",
                            provider.getName() + " - " + (reason != null ? reason : "未配置"),
                            new HealthAction("配置", "/settings/models")
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check providers: {}", e.getMessage());
            results.add(new HealthCheck(
                    "providers",
                    "warning",
                    "无法检查模型提供商: " + e.getMessage(),
                    null
            ));
        }
        return results;
    }

    private List<HealthCheck> checkMcpServers() {
        List<HealthCheck> results = new ArrayList<>();
        try {
            List<McpServerEntity> servers = mcpServerService.listAll();
            for (McpServerEntity server : servers) {
                if (!Boolean.TRUE.equals(server.getEnabled())) {
                    continue;
                }
                ConnectionResult cr = mcpClientManager.getConnectionResult(server.getId());
                if (cr == null || !cr.success()) {
                    String msg = server.getName() + " - "
                            + (cr != null ? cr.message() : "未连接");
                    results.add(new HealthCheck(
                            "mcp:" + server.getName(),
                            "warning",
                            msg,
                            new HealthAction("查看服务", "/settings/mcp-servers")
                    ));
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check MCP servers: {}", e.getMessage());
            results.add(new HealthCheck(
                    "mcp-servers",
                    "warning",
                    "无法检查 MCP 服务: " + e.getMessage(),
                    null
            ));
        }
        return results;
    }

    private HealthCheck checkDatabase() {
        if (bootstrapRunner.isInitialized()) {
            return new HealthCheck("数据库", "healthy", "数据库已初始化", null);
        }
        return new HealthCheck(
                "数据库",
                "error",
                "数据库未初始化",
                new HealthAction("设置", "/setup")
        );
    }

    private HealthCheck checkBrowser() {
        try {
            BrowserDiagnosticsService.Report report = browserDiagnostics.run();
            String status = switch (report.overall()) {
                case "healthy" -> "healthy";
                case "warning" -> "warning";
                default -> "error";
            };
            String message = "healthy".equals(report.overall())
                    ? "浏览器启动就绪"
                    : String.join(" | ", report.advice());
            HealthAction action = "healthy".equals(report.overall())
                    ? null
                    : new HealthAction("诊断", "/api/v1/system/browser-health");
            return new HealthCheck("浏览器", status, message, action);
        } catch (Exception e) {
            log.warn("Browser diagnostics failed: {}", e.getMessage());
            return new HealthCheck("浏览器", "warning",
                    "浏览器诊断失败: " + e.getMessage(), null);
        }
    }

    // ==================== Response Records ====================

    public record HealthResponse(String overall, List<HealthCheck> checks) {}

    public record HealthCheck(String name, String status, String message, HealthAction action) {}

    public record HealthAction(String label, String route) {}
}
