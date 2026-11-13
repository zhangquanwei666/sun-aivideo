package com.sun.aivideo.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.aivideo.admin.model.PageResponse;
import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.common.enums.UserStatus;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.dao.UserMapper;
import com.sun.aivideo.dao.bo.UserListBO;
import com.sun.aivideo.dao.bo.UserOverviewBO;
import com.sun.aivideo.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserMapper userMapper;
    public PageResponse<UserListBO> page(long pageNum, long pageSize, String keyword, String status) {
        var page = userMapper.selectUserPage(Page.of(pageNum, pageSize), trim(keyword), trim(status));
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

    private User requiredUser(long userId) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND, userId);
        }
        return user;
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
