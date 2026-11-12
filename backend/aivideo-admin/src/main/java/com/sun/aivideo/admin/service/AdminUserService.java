package com.sun.aivideo.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.aivideo.admin.auth.AdminStpUtil;
import com.sun.aivideo.admin.model.PageResponse;
import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.common.enums.MemberAction;
import com.sun.aivideo.common.enums.MemberLevel;
import com.sun.aivideo.common.enums.UserStatus;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.dao.AdminMapper;
import com.sun.aivideo.dao.MemberRecordMapper;
import com.sun.aivideo.dao.UserMapper;
import com.sun.aivideo.dao.bo.MemberRecordBO;
import com.sun.aivideo.dao.bo.UserListBO;
import com.sun.aivideo.dao.bo.UserOverviewBO;
import com.sun.aivideo.entity.Admin;
import com.sun.aivideo.entity.MemberRecord;
import com.sun.aivideo.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;
    private final AdminMapper adminMapper;
    private final MemberRecordMapper memberRecordMapper;

    public PageResponse<UserListBO> page(long pageNum, long pageSize, String keyword, String member, String status) {
        var page = userMapper.selectUserPage(Page.of(pageNum, pageSize), trim(keyword), trim(member), trim(status));
        return PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    public UserOverviewBO overview() {
        return userMapper.selectOverview();
    }

    @Transactional
    public BaseResponse changeStatus(long userId, String status) {
        if (!UserStatus.ENABLED.name().equals(status) && !UserStatus.DISABLED.name().equals(status)) {
            throw new BizException(ErrorCode.INVALID_USER_STATUS);
        }
        User user = requiredUser(userId);
        User update = new User();
        update.setId(user.getId());
        update.setStatus(status);
        userMapper.updateById(update);
        return BaseResponse.ok();
    }

    @Transactional
    public BaseResponse openMembership(long userId, Integer days, String remark) {
        if (days != null && days <= 0) {
            throw new BizException(ErrorCode.INVALID_MEMBER_DAYS);
        }
        User user = requiredUser(userId);
        LocalDateTime before = user.getMemberExpireAt();
        LocalDateTime after = days == null ? null : effectiveBase(user).plusDays(days);

        userMapper.updateMembership(userId, MemberLevel.VIP.name(), after);

        saveMemberRecord(userId, MemberAction.OPEN, MemberLevel.VIP, days, before, after, remark);
        return BaseResponse.ok();
    }

    @Transactional
    public BaseResponse revokeMembership(long userId, String remark) {
        User user = requiredUser(userId);
        LocalDateTime before = user.getMemberExpireAt();

        userMapper.updateMembership(userId, MemberLevel.NORMAL.name(), null);

        saveMemberRecord(userId, MemberAction.REVOKE, MemberLevel.NORMAL, null, before, null, remark);
        return BaseResponse.ok();
    }

    public PageResponse<MemberRecordBO> memberRecords(long pageNum, long pageSize, Long userId) {
        var page = memberRecordMapper.selectRecordPage(Page.of(pageNum, pageSize), userId);
        return PageResponse.of(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    private User requiredUser(long userId) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, userId);
        }
        return user;
    }

    private LocalDateTime effectiveBase(User user) {
        LocalDateTime now = LocalDateTime.now();
        return MemberLevel.isActive(user.getMemberLevel(), user.getMemberExpireAt())
                && user.getMemberExpireAt() != null ? user.getMemberExpireAt() : now;
    }

    private void saveMemberRecord(long userId, MemberAction action, MemberLevel level, Integer days,
                                  LocalDateTime before, LocalDateTime after, String remark) {
        Admin operator = adminMapper.selectById(AdminStpUtil.getLoginIdAsLong());
        MemberRecord record = new MemberRecord();
        record.setUserId(userId);
        record.setAction(action.name());
        record.setMemberLevel(level.name());
        record.setDays(days);
        record.setExpireBefore(before);
        record.setExpireAfter(after);
        record.setOperatorId(operator.getId());
        record.setOperatorName(operator.getUsername());
        record.setRemark(trim(remark));
        memberRecordMapper.insert(record);
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
