package com.sun.aivideo.admin;

import com.sun.aivideo.admin.controller.AdminUserController;
import com.sun.aivideo.admin.service.AdminFeedService;
import com.sun.aivideo.admin.service.AdminUserService;
import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.dao.FeedMapper;
import com.sun.aivideo.dao.ImageTaskMapper;
import com.sun.aivideo.dao.UserMapper;
import com.sun.aivideo.dao.VideoTaskMapper;
import com.sun.aivideo.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminUserServiceTest {

    @Test
    void changingStatusOnlyUpdatesTheAccountStatus() {
        UserMapper users = mock(UserMapper.class);
        AdminUserService service = new AdminUserService(users);
        User user = new User();
        user.setId(7L);
        when(users.selectByIdForUpdate(7L)).thenReturn(user);
        service.changeStatus(7L, "DISABLED");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).updateById(captor.capture());
        assertEquals(7L, captor.getValue().getId());
        assertEquals("DISABLED", captor.getValue().getStatus());
    }

    @Test
    void invalidOrMissingAccountNeverChangesStatus() {
        UserMapper users = mock(UserMapper.class);
        AdminUserService service = new AdminUserService(users);
        assertEquals(ErrorCode.INVALID_USER_STATUS, assertThrows(BizException.class,
                () -> service.changeStatus(7L, "invalid")).getErrorCode());
        assertEquals(ErrorCode.USER_NOT_FOUND, assertThrows(BizException.class,
                () -> service.changeStatus(7L, "ENABLED")).getErrorCode());
        verify(users, never()).updateById(any(User.class));
    }

    @Test
    void retiredAccountUpgradeRoutesAreNotExposed() throws Exception {
        AdminUserService service = mock(AdminUserService.class);
        var mvc = MockMvcBuilders.standaloneSetup(new AdminUserController(service)).build();
        mvc.perform(post("/api/admin/users/7/membership").contentType("application/json").content("{\"days\":30}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/users/7/membership/revoke"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/member-records"))
                .andExpect(status().isNotFound());
        verifyNoInteractions(service);
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
