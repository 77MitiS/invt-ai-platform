package vip.mate.enterprise;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vip.mate.common.result.R;
import vip.mate.workspace.core.annotation.RequireWorkspaceRole;

import java.util.*;

/**
 * Enterprise workbench REST API — contract review & account intel.
 */
@Tag(name = "企业工作台")
@RestController
@RequestMapping("/api/v1/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseDataService service;

    // ==================== Contracts ====================

    @Operation(summary = "获取合同列表")
    @GetMapping("/contracts")
    @RequireWorkspaceRole("member")
    public R<List<Map<String, Object>>> listContracts() {
        return R.ok(service.getContracts());
    }

    @Operation(summary = "保存合同")
    @PostMapping("/contracts")
    @RequireWorkspaceRole("admin")
    public R<Map<String, Object>> saveContract(@RequestBody Map<String, Object> contract) {
        return R.ok(service.saveContract(contract));
    }

    @Operation(summary = "删除合同")
    @DeleteMapping("/contracts/{id}")
    @RequireWorkspaceRole("admin")
    public R<Void> deleteContract(@PathVariable String id) {
        service.deleteContract(id);
        return R.ok();
    }

    // ==================== Accounts ====================

    @Operation(summary = "获取客户列表")
    @GetMapping("/accounts")
    @RequireWorkspaceRole("member")
    public R<List<Map<String, Object>>> listAccounts() {
        return R.ok(service.getAccounts());
    }

    @Operation(summary = "保存客户")
    @PostMapping("/accounts")
    @RequireWorkspaceRole("admin")
    public R<Map<String, Object>> saveAccount(@RequestBody Map<String, Object> account) {
        return R.ok(service.saveAccount(account));
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/accounts/{id}")
    @RequireWorkspaceRole("admin")
    public R<Void> deleteAccount(@PathVariable String id) {
        service.deleteAccount(id);
        return R.ok();
    }
}
