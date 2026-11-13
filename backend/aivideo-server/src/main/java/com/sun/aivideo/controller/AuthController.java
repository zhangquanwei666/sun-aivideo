package com.sun.aivideo.controller;
import com.sun.aivideo.common.enums.ErrorCode;
import cn.dev33.satoken.stp.StpUtil;
import com.sun.aivideo.dao.UserMapper;
import com.sun.aivideo.entity.User;
import com.sun.aivideo.common.base.BaseResponse;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.model.vo.AuthVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Map;
@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
 private final UserMapper users;
 private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
 public record Credentials(@NotBlank @Pattern(regexp="[a-zA-Z0-9_]{4,32}") String username,
   @NotBlank @Size(min=8,max=64) String password) {}
 @PostMapping("/register") @Transactional public AuthVO register(@Valid @RequestBody Credentials dto, HttpServletRequest request) {
   if(dto.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new BizException(ErrorCode.PASSWORD_TOO_LONG);
   User u=new User(); u.setUsername(dto.username()); u.setPasswordHash(encoder.encode(dto.password()));
   u.setNickname(dto.username()); u.setStatus("ENABLED"); u.setDeleted(false); u.setRegisterIp(request.getRemoteAddr());
   try { users.insert(u); } catch(DuplicateKeyException e) { throw new BizException(ErrorCode.USERNAME_EXISTS); }
   return createSession(u);
 }
 @PostMapping("/login") public AuthVO login(@Valid @RequestBody Credentials dto,HttpServletRequest request) {
   User u=users.selectByUsername(dto.username());
   if(u==null || !encoder.matches(dto.password(),u.getPasswordHash())) throw new BizException(ErrorCode.INVALID_CREDENTIALS);
   if(!"ENABLED".equals(u.getStatus())) throw new BizException(ErrorCode.ACCOUNT_DISABLED);
   User update=new User(); update.setId(u.getId()); update.setLastLoginAt(LocalDateTime.now()); update.setLastLoginIp(request.getRemoteAddr()); users.updateById(update);
   return createSession(u);
 }
 @GetMapping("/me") public Map<String,Object> me() {
   User u=users.selectById(StpUtil.getLoginIdAsLong());
   return Map.of("code",0,"message","success","id",u.getId().toString(),"username",u.getUsername());
 }
 @PostMapping("/logout") public BaseResponse logout() { StpUtil.logout(); return BaseResponse.ok(); }

 private AuthVO createSession(User user) {
   StpUtil.login(user.getId());
   AuthVO response = new AuthVO();
   response.setToken(StpUtil.getTokenValue());
   response.setUsername(user.getUsername());
   return BaseResponse.ok(response);
 }
}
