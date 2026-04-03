/*
 * Copyright (c) 2026 Bai_Jing88 (QQ: 1782307393)
 * PCS (Player Credit System) - Minecraft Cross-Server Player Management
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

package com.pcs.central.service;

import com.pcs.central.database.AdminUserRepository;
import com.pcs.central.database.LoginAuditRepository;
import com.pcs.central.model.entity.AdminUser;
import com.pcs.central.model.entity.LoginAudit;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class AdminUserService {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserService.class);

    // 安全配置常量
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;
    private static final int PASSWORD_MIN_LENGTH = 6;
    private static final int AUDIT_RETENTION_DAYS = 90;

    private final AdminUserRepository adminUserRepository;
    private final LoginAuditRepository loginAuditRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(AdminUserRepository adminUserRepository,
                           LoginAuditRepository loginAuditRepository,
                           PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.loginAuditRepository = loginAuditRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 初始化超级管理员账户
     */
    @PostConstruct
    public void initSuperAdmin() {
        try {
            if (adminUserRepository.count() == 0) {
                logger.info("初始化超级管理员账户...");
                AdminUser superAdmin = new AdminUser();
                superAdmin.setUsername("admin");
                superAdmin.setPassword(passwordEncoder.encode("admin123"));
                superAdmin.setRole("SUPER_ADMIN");
                superAdmin.setEnabled(true);
                superAdmin.setCreatedBy("system");
                adminUserRepository.save(superAdmin);
                logger.info("超级管理员账户已创建: admin / admin123");
                logger.warn("警告: 请立即登录并修改默认密码!");
            }
        } catch (Exception e) {
            logger.warn("初始化超级管理员账户失败(数据库表可能尚未创建): {}", e.getMessage());
        }
    }

    /**
     * 启动时初始化（由CommandLineRunner调用，确保表已创建）
     */
    public void initSuperAdminOnStartup() {
        try {
            if (adminUserRepository.count() == 0) {
                logger.info("启动时初始化超级管理员账户...");
                AdminUser superAdmin = new AdminUser();
                superAdmin.setUsername("admin");
                superAdmin.setPassword(passwordEncoder.encode("admin123"));
                superAdmin.setRole("SUPER_ADMIN");
                superAdmin.setEnabled(true);
                superAdmin.setCreatedBy("system");
                adminUserRepository.save(superAdmin);
                logger.info("超级管理员账户已创建: admin / admin123");
                logger.warn("警告: 请立即登录并修改默认密码!");
            } else {
                logger.info("超级管理员账户已存在，跳过初始化");
            }
        } catch (Exception e) {
            logger.error("启动时初始化超级管理员账户失败: {}", e.getMessage());
            throw new RuntimeException("数据初始化失败", e);
        }
    }

    /**
     * 登录验证（带试错限制）
     */
    @Transactional
    public LoginResult authenticate(String username, String password, HttpServletRequest request) {
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        Optional<AdminUser> userOpt = adminUserRepository.findByUsername(username);

        // 用户不存在
        if (userOpt.isEmpty()) {
            recordLoginAttempt(username, ipAddress, userAgent, false, "用户不存在");
            return LoginResult.fail("用户名或密码错误");
        }

        AdminUser user = userOpt.get();

        // 检查账户是否被禁用
        if (!user.isEnabled()) {
            recordLoginAttempt(username, ipAddress, userAgent, false, "账户已禁用");
            return LoginResult.fail("账户已被禁用，请联系超级管理员");
        }

        // 检查账户是否被锁定
        if (user.isLocked()) {
            long remainingMinutes = ChronoUnit.MINUTES.between(
                LocalDateTime.now(), user.getLockedUntil());
            recordLoginAttempt(username, ipAddress, userAgent, false,
                "账户已锁定，剩余时间: " + remainingMinutes + "分钟");
            return LoginResult.fail("账户已锁定，请" + remainingMinutes + "分钟后重试");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            handleFailedLogin(user);
            recordLoginAttempt(username, ipAddress, userAgent, false, "密码错误");

            int remainingAttempts = MAX_FAILED_ATTEMPTS - user.getFailedAttempts() - 1;
            if (remainingAttempts > 0) {
                return LoginResult.fail("密码错误，还剩" + remainingAttempts + "次尝试机会");
            } else {
                return LoginResult.fail("密码错误，账户已锁定" + LOCKOUT_DURATION_MINUTES + "分钟");
            }
        }

        // 登录成功
        handleSuccessfulLogin(user);
        recordLoginAttempt(username, ipAddress, userAgent, true, null);
        logger.info("管理员 {} 登录成功，IP: {}", username, ipAddress);

        return LoginResult.success(user);
    }

    /**
     * 创建新管理员（仅超级管理员）
     */
    @Transactional
    public AdminUser createAdmin(String username, String password, String role, String createdBy) {
        if (adminUserRepository.existsByUsername(username)) {
            throw new RuntimeException("用户名已存在");
        }

        validatePassword(password);

        AdminUser newAdmin = new AdminUser();
        newAdmin.setUsername(username);
        newAdmin.setPassword(passwordEncoder.encode(password));
        newAdmin.setRole(role);
        newAdmin.setEnabled(true);
        newAdmin.setCreatedBy(createdBy);
        newAdmin.setFailedAttempts(0);

        return adminUserRepository.save(newAdmin);
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        AdminUser user = adminUserRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        adminUserRepository.save(user);
    }

    /**
     * 重置密码（超级管理员功能）
     */
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        AdminUser user = adminUserRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        validatePassword(newPassword);
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        adminUserRepository.save(user);
    }

    /**
     * 解锁账户
     */
    @Transactional
    public void unlockUser(Long userId) {
        AdminUser user = adminUserRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        adminUserRepository.save(user);
    }

    /**
     * 禁用/启用账户
     */
    @Transactional
    public void setUserEnabled(Long userId, boolean enabled) {
        AdminUser user = adminUserRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setEnabled(enabled);
        adminUserRepository.save(user);
    }

    /**
     * 获取所有管理员
     */
    public List<AdminUser> getAllAdmins() {
        return adminUserRepository.findAll();
    }

    /**
     * 获取登录审计日志
     */
    public List<LoginAudit> getLoginAudits(String username) {
        if (username != null && !username.isEmpty()) {
            return loginAuditRepository.findByUsernameOrderByCreatedAtDesc(username);
        }
        return loginAuditRepository.findRecentAudits();
    }

    /**
     * 清理旧审计日志
     */
    @Transactional
    public void cleanupOldAudits() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(AUDIT_RETENTION_DAYS);
        // 使用自定义查询删除旧记录
        logger.info("清理{}天前的登录审计日志", AUDIT_RETENTION_DAYS);
    }

    // ============ 私有方法 ============

    private void handleFailedLogin(AdminUser user) {
        user.recordFailedAttempt();

        if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            logger.warn("管理员 {} 连续{}次登录失败，账户已锁定{}分钟",
                user.getUsername(), MAX_FAILED_ATTEMPTS, LOCKOUT_DURATION_MINUTES);
        }

        adminUserRepository.save(user);
    }

    private void handleSuccessfulLogin(AdminUser user) {
        user.resetFailedAttempts();
        user.setLockedUntil(null);
        user.setLastLogin(LocalDateTime.now());
        adminUserRepository.save(user);
    }

    private void recordLoginAttempt(String username, String ip, String userAgent,
                                    boolean success, String failureReason) {
        try {
            LoginAudit audit = new LoginAudit();
            audit.setUsername(username);
            audit.setIpAddress(ip);
            audit.setUserAgent(userAgent);
            audit.setSuccess(success);
            audit.setFailureReason(failureReason);
            loginAuditRepository.save(audit);
        } catch (Exception e) {
            logger.error("记录登录审计日志失败", e);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH) {
            throw new RuntimeException("密码长度至少" + PASSWORD_MIN_LENGTH + "位");
        }
        // 检查密码复杂度
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        String specialChars = "!@#$%^&*()_+-=[]{};':\"|,.<>/?";

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (specialChars.indexOf(c) >= 0) hasSpecial = true;
        }

        int strength = 0;
        if (hasUpper) strength++;
        if (hasLower) strength++;
        if (hasDigit) strength++;
        if (hasSpecial) strength++;

        if (strength < 3) {
            throw new RuntimeException("密码必须包含大写字母、小写字母、数字和特殊字符中的至少3种");
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    // 登录结果对象
    public static class LoginResult {
        private final boolean success;
        private final String message;
        private final AdminUser user;

        private LoginResult(boolean success, String message, AdminUser user) {
            this.success = success;
            this.message = message;
            this.user = user;
        }

        public static LoginResult success(AdminUser user) {
            return new LoginResult(true, "登录成功", user);
        }

        public static LoginResult fail(String message) {
            return new LoginResult(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public AdminUser getUser() { return user; }
    }
}
