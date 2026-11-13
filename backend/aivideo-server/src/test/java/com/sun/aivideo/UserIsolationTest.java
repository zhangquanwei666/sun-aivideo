package com.sun.aivideo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import cn.dev33.satoken.stp.StpUtil;
import com.sun.aivideo.dao.*;
import com.sun.aivideo.entity.*;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.common.enums.ErrorCode;
import com.sun.aivideo.service.impl.*;
import com.sun.aivideo.controller.AuthController;
import org.springframework.mock.web.MockHttpServletRequest;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
class UserIsolationTest {
 @Test void errorCodeFormatsDynamicMessage() {
  var error = new BizException(ErrorCode.VIDEO_NOT_FOUND, 42L);
  assertEquals(404, error.getCode());
  assertEquals("任务不存在：42", error.getMessage());
  assertSame(ErrorCode.VIDEO_NOT_FOUND, error.getErrorCode());
 }

 @Test void rejectsOtherUsersVideo() {
  var mapper=mock(VideoTaskMapper.class); var task=new VideoTask(); task.setUserId(2L); when(mapper.selectById(10L)).thenReturn(task);
  var service=new VideoGenerationServiceImpl(mapper,null,null,null,null,null,null,null,null);
  try(var session=mockStatic(StpUtil.class)) { session.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
   assertThrows(BizException.class,()->service.detail(10L)); assertThrows(BizException.class,()->service.delete(10L));
   verify(mapper,never()).softDeleteById(any(),any());
  }
 }
 @Test void rejectsOtherUsersImageAndLegacyRecord() {
  var mapper=mock(ImageTaskMapper.class); var task=new ImageTask(); when(mapper.selectById(10L)).thenReturn(task);
  var service=new ImageGenerationServiceImpl(mapper,null,null,null,null,null,null,null,null);
  try(var session=mockStatic(StpUtil.class)) { session.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
   assertThrows(BizException.class,()->service.detail(10L)); task.setUserId(2L);
   assertThrows(BizException.class,()->service.delete(10L)); verify(mapper,never()).softDeleteById(any(),any());
  }
 }
 @Test void registrationHashesPasswordAndDefaultsToEnabled() {
  var mapper=mock(UserMapper.class); var auth=new AuthController(mapper);
  doAnswer(invocation -> { invocation.<User>getArgument(0).setId(7L); return 1; }).when(mapper).insert(any(User.class));
  try(var session=mockStatic(StpUtil.class)) {
   session.when(StpUtil::getTokenValue).thenReturn("new-user-token");
   var response=auth.register(new AuthController.Credentials("testuser","secret123"),new MockHttpServletRequest());
   var capture=ArgumentCaptor.forClass(User.class); verify(mapper).insert(capture.capture());
   var user=capture.getValue(); assertEquals("ENABLED",user.getStatus());
   assertNotEquals("secret123",user.getPasswordHash()); assertTrue(new BCryptPasswordEncoder().matches("secret123",user.getPasswordHash()));
   assertEquals("new-user-token",response.getToken()); assertEquals("testuser",response.getUsername());
   session.verify(() -> StpUtil.login(7L));
  }
 }
 @Test void wrongPasswordNeverCreatesSession() {
  var mapper=mock(UserMapper.class); var auth=new AuthController(mapper);
  try(var session=mockStatic(StpUtil.class)) {
   assertThrows(BizException.class,()->auth.login(new AuthController.Credentials("missing","secret123"),new MockHttpServletRequest())); session.verifyNoInteractions();
  }
 }
 @Test void currentUserResponseOnlyContainsPublicIdentity() {
  var mapper=mock(UserMapper.class); var user=new User(); user.setId(7L); user.setUsername("testuser"); user.setPasswordHash("private-password-hash");
  when(mapper.selectById(7L)).thenReturn(user);
  var auth=new AuthController(mapper);
  try(var session=mockStatic(StpUtil.class)) {
   session.when(StpUtil::getLoginIdAsLong).thenReturn(7L);
   var response=auth.me();
   assertEquals(java.util.Set.of("code","message","id","username"),response.keySet());
   assertEquals("7",response.get("id")); assertEquals("testuser",response.get("username"));
  }
 }
}
