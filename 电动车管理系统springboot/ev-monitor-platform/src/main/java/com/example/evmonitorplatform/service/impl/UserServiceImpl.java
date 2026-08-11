package com.example.evmonitorplatform.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.evmonitorplatform.common.exception.BusinessException;
import com.example.evmonitorplatform.common.response.PageResponse;
import com.example.evmonitorplatform.common.utils.JwtUtil;
import com.example.evmonitorplatform.entity.OrgClass;
import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.mapper.OrgClassMapper;
import com.example.evmonitorplatform.mapper.UserMapper;
import com.example.evmonitorplatform.service.UserService;
import com.example.evmonitorplatform.vo.request.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OrgClassMapper orgClassMapper;

    @Override
    public User login(LoginVO loginVO) {
        String username = loginVO.getUsername() == null ? null : loginVO.getUsername().trim();
        if (!StringUtils.hasText(username)) {
            throw new BusinessException("Username is required");
        }

        User user = baseMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        if (!verifyAndUpgradeLegacyPassword(user, loginVO.getPassword())) {
            throw new BusinessException("Invalid username or password");
        }

        if ("frozen".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException("Account is frozen");
        }
        if ("deleted".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException("Account is deleted");
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getUsername(), user.getUserType());
        user.setPassword(null);
        user.setToken(token);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(User user) {
        normalizeUserFields(user);

        if (!StringUtils.hasText(user.getUsername())) {
            throw new BusinessException("Username is required");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new BusinessException("Password is required");
        }
        applyRelaxedRegisterDefaults(user);

        if ("student".equalsIgnoreCase(user.getUserType())) {
            user.setJobId(null);
            fillMissingStudentOrgFields(user);
        }

        if (baseMapper.selectByUsername(user.getUsername()) != null) {
            throw new BusinessException("Username already exists");
        }
        if (baseMapper.selectByPhone(user.getPhone()) != null) {
            throw new BusinessException("Phone already registered");
        }
        if (StringUtils.hasText(user.getStudentId()) && baseMapper.selectByStudentId(user.getStudentId()) != null) {
            throw new BusinessException("Student ID already exists");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus("active");
        user.setLicensePoints(12);
        user.setDeleted(false);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        if (!this.save(user)) {
            throw new BusinessException("Register failed");
        }

        User saved = this.getById(user.getUserId());
        if (saved != null) {
            saved.setPassword(null);
            return saved;
        }

        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updatePassword(Integer userId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new BusinessException("New password must be at least 6 characters");
        }

        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        if (!verifyAndUpgradeLegacyPassword(user, oldPassword)) {
            throw new BusinessException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        return this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetPassword(Integer userId, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new BusinessException("New password must be at least 6 characters");
        }

        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        return this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProfile(User user) {
        User existingUser = baseMapper.selectById(user.getUserId());
        if (existingUser == null) {
            throw new BusinessException("User not found");
        }

        normalizeUserFields(user);

        user.setPassword(null);
        user.setUsername(null);
        user.setUserType(null);
        user.setCreateTime(null);
        user.setUpdateTime(LocalDateTime.now());

        return this.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Integer userId, String status) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        return this.updateById(user);
    }

    @Override
    public PageResponse<List<User>> queryUsers(String keyword, String userType, String status,
                                               Integer pageNum, Integer pageSize) {
        Page<User> page = new Page<>(pageNum, pageSize);
        Page<User> resultPage = (Page<User>) baseMapper.selectUserPage(page, keyword, userType, status);
        resultPage.getRecords().forEach(item -> item.setPassword(null));

        return new PageResponse<>(
                200,
                "success",
                resultPage.getRecords(),
                (int) resultPage.getCurrent(),
                (int) resultPage.getSize(),
                resultPage.getTotal()
        );
    }

    @Override
    public Map<String, Object> getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", this.count());
        stats.put("typeStats", baseMapper.countByUserType());
        stats.put("statusStats", baseMapper.countByStatus());
        return stats;
    }

    @Override
    public Map<String, Object> getUserDashboard(Integer userId) {
        Map<String, Object> dashboard = new HashMap<>();
        User user = this.getById(userId);
        if (user != null) {
            user.setPassword(null);
            dashboard.put("userInfo", user);
            int maxP = 12;
            int rem = user.getLicensePoints() != null
                    ? Math.min(maxP, Math.max(0, user.getLicensePoints()))
                    : maxP;
            Map<String, Object> lic = new HashMap<>();
            lic.put("maxPoints", maxP);
            lic.put("remaining", rem);
            lic.put("usedPoints", maxP - rem);
            dashboard.put("licenseSummary", lic);
        }
        dashboard.put("stats", baseMapper.selectUserWithStats(userId));
        return dashboard;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignRole(Integer userId, String role) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        user.setUserType(role);
        user.setUpdateTime(LocalDateTime.now());
        return this.updateById(user);
    }

    @Override
    public User getById(Number id) {
        User user = super.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    private void normalizeUserFields(User user) {
        if (user == null) {
            return;
        }
        user.setUsername(trimToNull(user.getUsername()));
        user.setRealName(trimToNull(user.getRealName()));
        user.setPhone(trimToNull(user.getPhone()));
        user.setStudentId(trimToNull(user.getStudentId()));
        user.setJobId(trimToNull(user.getJobId()));
        user.setEmail(trimToNull(user.getEmail()));
        user.setDepartment(trimToNull(user.getDepartment()));
        user.setCollege(trimToNull(user.getCollege()));
        user.setMajor(trimToNull(user.getMajor()));
        user.setClassName(trimToNull(user.getClassName()));
        user.setUserType(trimToNull(user.getUserType()));
    }

    private void applyRelaxedRegisterDefaults(User user) {
        if (!StringUtils.hasText(user.getRealName())) {
            user.setRealName(user.getUsername());
        }
        if (!StringUtils.hasText(user.getUserType())) {
            user.setUserType("student");
        }
        String fallbackId = String.valueOf(Math.abs(user.getUsername().hashCode()));
        if (!StringUtils.hasText(user.getPhone())) {
            user.setPhone("PHONE_" + fallbackId);
        }
        if ("student".equalsIgnoreCase(user.getUserType())) {
            if (!StringUtils.hasText(user.getStudentId())) {
                user.setStudentId("SID_" + fallbackId);
            }
            if (!StringUtils.hasText(user.getClassName())) {
                user.setClassName("未填写班级");
            }
        }
    }

    private void fillMissingStudentOrgFields(User user) {
        boolean missingCollege = !StringUtils.hasText(user.getCollege());
        boolean missingMajor = !StringUtils.hasText(user.getMajor());
        if (!missingCollege && !missingMajor) {
            return;
        }

        try {
            OrgClass orgClass = orgClassMapper.selectOne(
                    Wrappers.<OrgClass>lambdaQuery()
                            .eq(OrgClass::getClassName, user.getClassName())
                            .last("LIMIT 1"));
            if (orgClass != null) {
                if (missingCollege && StringUtils.hasText(orgClass.getCollege())) {
                    user.setCollege(orgClass.getCollege().trim());
                    missingCollege = false;
                }
                if (missingMajor && StringUtils.hasText(orgClass.getMajor())) {
                    user.setMajor(orgClass.getMajor().trim());
                    missingMajor = false;
                }
            }
        } catch (Exception e) {
            log.warn("根据班级补全学院专业失败，className={}", user.getClassName(), e);
        }

        if (missingCollege) {
            user.setCollege("未填写学院");
        }
        if (missingMajor) {
            user.setMajor("未填写专业");
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * Backward-compatible password verification:
     * 1) prefer BCrypt match
     * 2) fallback to legacy plain-text equality
     * 3) if legacy plain-text matched, upgrade it to BCrypt immediately
     */
    private boolean verifyAndUpgradeLegacyPassword(User user, String rawPassword) {
        if (user == null || !StringUtils.hasText(rawPassword) || !StringUtils.hasText(user.getPassword())) {
            return false;
        }

        try {
            if (passwordEncoder.matches(rawPassword, user.getPassword())) {
                return true;
            }
        } catch (IllegalArgumentException ignored) {
            // Non-BCrypt historical password format, fallback below.
        }

        if (rawPassword.equals(user.getPassword())) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setUpdateTime(LocalDateTime.now());
            baseMapper.updateById(user);
            return true;
        }
        return false;
    }
}
