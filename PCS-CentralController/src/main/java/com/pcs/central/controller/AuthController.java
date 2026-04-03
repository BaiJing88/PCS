/*
 * Copyright (c) 2026 Bai_Jing88 (QQ: 1782307393)
 * PCS (Player Credit System) - Central Controller
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * 
 * Any derivative work must also be open source and licensed under
 * the same AGPL v3 license. Commercial use is prohibited without
 * explicit permission from the author.
 */

package com.pcs.central.controller;

import com.pcs.central.model.entity.AdminUser;
import com.pcs.central.security.JwtTokenProvider;
import com.pcs.central.service.AdminUserService;
import com.pcs.central.service.AdminUserService.LoginResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 * 处理管理员登录和账户管理
 * 
 * @author Bai_Jing88 (QQ: 1782307393)
 * @version 1.0.0
 * @since 2026
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminUserService adminUserService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(AdminUserService adminUserService, JwtTokenProvider tokenProvider) {
        this.adminUserService = adminUserService;
        this.tokenProvider = tokenProvider;
    }

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResult result = adminUserService.authenticate(
            request.getUsername(),
            request.getPassword(),
            httpRequest
        );

        if (result.isSuccess()) {
            AdminUser user = result.getUser();
            String token = tokenProvider.generateToken(user.getUsername(), user.getRole());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "token", token,
                "username", user.getUsername(),
                "role", user.getRole(),
                "isSuperAdmin", user.isSuperAdmin()
            ));
        } else {
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", result.getMessage()
            ));
        }
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request,
                                            @RequestAttribute("username") String username) {
        try {
            AdminUser user = adminUserService.getAllAdmins().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            adminUserService.changePassword(user.getId(), request.getOldPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("success", true, "message", "密码修改成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 创建管理员（仅超级管理员）
     */
    @PostMapping("/admin/create")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createAdmin(@RequestBody CreateAdminRequest request,
                                         @RequestAttribute("username") String createdBy) {
        try {
            AdminUser newAdmin = adminUserService.createAdmin(
                request.getUsername(),
                request.getPassword(),
                request.getRole(),
                createdBy
            );
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "管理员创建成功",
                "username", newAdmin.getUsername()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 获取管理员列表
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> listAdmins() {
        List<AdminUser> admins = adminUserService.getAllAdmins();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", admins.stream().map(a -> Map.of(
                "id", a.getId(),
                "username", a.getUsername(),
                "role", a.getRole(),
                "enabled", a.isEnabled(),
                "createdAt", a.getCreatedAt(),
                "lastLogin", a.getLastLogin(),
                "locked", a.isLocked()
            ))
        ));
    }

    /**
     * 重置密码
     */
    @PostMapping("/admin/{id}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> request) {
        try {
            String newPassword = request.get("newPassword");
            adminUserService.resetPassword(id, newPassword);
            return ResponseEntity.ok(Map.of("success", true, "message", "密码重置成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 启用/禁用账户
     */
    @PostMapping("/admin/{id}/toggle")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> toggleUser(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        try {
            boolean enabled = request.getOrDefault("enabled", false);
            adminUserService.setUserEnabled(id, enabled);
            return ResponseEntity.ok(Map.of("success", true, "message", enabled ? "账户已启用" : "账户已禁用"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 解锁账户
     */
    @PostMapping("/admin/{id}/unlock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> unlockUser(@PathVariable Long id) {
        try {
            adminUserService.unlockUser(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "账户已解锁"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 获取审计日志
     */
    @GetMapping("/audit/logs")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> getAuditLogs(@RequestParam(required = false) String username) {
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", adminUserService.getLoginAudits(username)
        ));
    }

    // Request DTOs
    public static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class ChangePasswordRequest {
        private String oldPassword;
        private String newPassword;
        public String getOldPassword() { return oldPassword; }
        public void setOldPassword(String oldPassword) { this.oldPassword = oldPassword; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class CreateAdminRequest {
        private String username;
        private String password;
        private String role;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
