package com.sun.aivideo.admin;

import com.sun.aivideo.admin.auth.AdminStpUtil;
import com.sun.aivideo.admin.service.AdminFeedService;
import com.sun.aivideo.admin.service.AdminUserService;
import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.common.enums.MemberLevel;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.dao.AdminMapper;
import com.sun.aivideo.dao.FeedMapper;
import com.sun.aivideo.dao.ImageTaskMapper;
import com.sun.aivideo.dao.MemberRecordMapper;
import com.sun.aivideo.dao.UserMapper;
import com.sun.aivideo.dao.VideoTaskMapper;
import com.sun.aivideo.entity.Admin;
import com.sun.aivideo.entity.MemberRecord;
import com.sun.aivideo.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTest {

    @Test
    void membershipRenewalStartsAtCurrentExpiryAndWritesAuditRecord() {
        UserMapper users = mock(UserMapper.class);
        AdminMapper admins = mock(AdminMapper.class);
        MemberRecordMapper records = mock(MemberRecordMapper.class);
        AdminUserService service = new AdminUserService(users, admins, records);

        LocalDateTime currentExpiry = LocalDateTime.now().plusDays(2);
        User user = new User();
        user.setId(7L);
        user.setMemberLevel(MemberLevel.VIP.name());
        user.setMemberExpireAt(currentExpiry);
        when(users.selectByIdForUpdate(7L)).thenReturn(user);

        Admin admin = new Admin();
        admin.setId(1L);
        admin.setUsername("admin");
        when(admins.selectById(1L)).thenReturn(admin);

        try (var login = mockStatic(AdminStpUtil.class)) {
            login.when(AdminStpUtil::getLoginIdAsLong).thenReturn(1L);
            service.openMembership(7L, 30, "续期");
        }

        ArgumentCaptor<MemberRecord> captor = ArgumentCaptor.forClass(MemberRecord.class);
        verify(records).insert(captor.capture());
        assertEquals(currentExpiry.plusDays(30), captor.getValue().getExpireAfter());
        assertEquals("admin", captor.getValue().getOperatorName());
        verify(users).updateMembership(7L, MemberLevel.VIP.name(), currentExpiry.plusDays(30));
    }

    @Test
    void invalidFeedTypeUsesCentralErrorCode() {
        AdminFeedService service = new AdminFeedService(mock(FeedMapper.class), mock(VideoTaskMapper.class),
                mock(ImageTaskMapper.class), mock(UserMapper.class));
        BizException error = assertThrows(BizException.class,
                () -> service.page(1, 20, null, "audio", null));
        assertEquals(ErrorCode.INVALID_FEED_TYPE, error.getErrorCode());
    }
}
