package com.sun.aivideo;

import com.sun.aivideo.common.enums.AiChannel;
import com.sun.aivideo.common.exception.BizException;
import com.sun.aivideo.config.CredentialCipher;
import com.sun.aivideo.dao.*;
import com.sun.aivideo.entity.*;
import com.sun.aivideo.service.ChannelCredentialService;
import com.sun.aivideo.service.UserInvocationGuard;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChannelCredentialTest {
    private static final String MASTER = Base64.getEncoder().encodeToString(new byte[32]);

    @Test void authenticatedEncryptionBindsCiphertextToOwnerAndUsesRandomNonce() {
        var cipher = new CredentialCipher(MASTER, "v1");
        String one = cipher.encrypt("private-example-key", "1:DASHSCOPE");
        assertNotEquals(one, cipher.encrypt("private-example-key", "1:DASHSCOPE"));
        assertFalse(one.contains("private-example-key"));
        assertEquals("private-example-key", cipher.decrypt(one, "1:DASHSCOPE", "v1"));
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(one, "2:DASHSCOPE", "v1"));
        byte[] tampered = Base64.getDecoder().decode(one); tampered[15] ^= 1;
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(Base64.getEncoder().encodeToString(tampered), "1:DASHSCOPE", "v1"));
        assertThrows(IllegalStateException.class, () -> cipher.decrypt(one, "1:DASHSCOPE", "v2"));
        assertThrows(IllegalStateException.class, () -> new CredentialCipher("", "v1"));
    }

    @Test void rotationPinsOldTasksAndDisableOnlyBlocksNewTasks() {
        var f = new Fixture();
        f.service.save(1L, "DASHSCOPE", "alice-old-key");
        Long oldVersion = f.service.requiredVersion(1L, "DASHSCOPE");
        f.service.save(1L, "DASHSCOPE", "alice-new-key");
        Long newVersion = f.service.requiredVersion(1L, "DASHSCOPE");
        assertNotEquals(oldVersion, newVersion);
        assertEquals("alice-old-key", f.service.forTask(1L, "DASHSCOPE", oldVersion).keyFor(AiChannel.DASHSCOPE));
        assertEquals("alice-new-key", f.service.forTask(1L, "DASHSCOPE", newVersion).keyFor(AiChannel.DASHSCOPE));
        f.service.setEnabled(1L, "DASHSCOPE", false);
        assertThrows(BizException.class, () -> f.service.requiredVersion(1L, "DASHSCOPE"));
        assertEquals("alice-old-key", f.service.forTask(1L, "DASHSCOPE", oldVersion).keyFor(AiChannel.DASHSCOPE));
        assertThrows(BizException.class, () -> f.service.forTask(2L, "DASHSCOPE", oldVersion));
        assertThrows(BizException.class, () -> f.service.forTask(1L, "GEMINI", oldVersion));
        assertFalse(f.service.list(1L).toString().contains("alice-new-key"));
        assertTrue(f.service.list(1L).getFirst().maskedKey().endsWith("-key"));
    }

    @Test void twoUsersAndChannelsHaveIndependentConfigurations() {
        var f = new Fixture();
        f.service.save(1L, "DASHSCOPE", "alice-dashscope");
        f.service.save(1L, "GEMINI", "alice-gemini");
        f.service.save(2L, "DASHSCOPE", "bob-dashscope");
        assertNotEquals(f.service.requiredVersion(1L, "DASHSCOPE"), f.service.requiredVersion(2L, "DASHSCOPE"));
        assertEquals("alice-gemini", f.service.forTask(1L, "GEMINI", f.service.requiredVersion(1L, "GEMINI")).keyFor(AiChannel.GEMINI));
        assertThrows(BizException.class, () -> f.service.requiredVersion(2L, "GEMINI"));
        assertThrows(BizException.class, () -> f.service.save(1L, "QWEN", "some-key-value"));
    }

    @Test void deletingInUseCredentialIsRejectedWithoutErasingAnything() {
        var f = new Fixture(); f.service.save(1L, "DASHSCOPE", "alice-dashscope");
        when(f.configs.activeReferences(anyLong())).thenReturn(1L);
        assertThrows(BizException.class, () -> f.service.delete(1L, "DASHSCOPE"));
        verify(f.configs, never()).clearCurrent(anyLong());
        assertThrows(BizException.class, () -> f.service.delete(2L, "DASHSCOPE"));
        verify(f.configs, never()).clearCurrent(anyLong());
        when(f.configs.activeReferences(anyLong())).thenReturn(0L);
        f.service.delete(1L, "DASHSCOPE"); verify(f.configs).clearCurrent(anyLong());
    }

    @Test void perUserConcurrencyIsBoundedAndPermitsAreReleasedOnFailure() {
        var guard = new UserInvocationGuard(1);
        try (var permit = guard.acquire(1L)) {
            assertThrows(RuntimeException.class, () -> guard.acquire(1L));
            try (var other = guard.acquire(2L)) { assertNotNull(other); }
        }
        try (var again = guard.acquire(1L)) { assertNotNull(again); }
    }

    private static class Fixture {
        final ChannelCredentialMapper configs = mock(ChannelCredentialMapper.class);
        final ChannelCredentialVersionMapper versions = mock(ChannelCredentialVersionMapper.class);
        final UserMapper users = mock(UserMapper.class);
        final Map<Long, ChannelCredential> configRows = new HashMap<>();
        final Map<Long, ChannelCredentialVersion> versionRows = new HashMap<>();
        final ChannelCredentialService service = new ChannelCredentialService(configs, versions, users, new CredentialCipher(MASTER, "v1"));
        Fixture() {
            AtomicLong ids = new AtomicLong();
            when(users.selectByIdForUpdate(anyLong())).thenAnswer(i -> { var u = new User(); u.setId(i.getArgument(0)); u.setStatus("ENABLED"); return u; });
            when(configs.find(anyLong(), anyString())).thenAnswer(i -> configRows.values().stream()
                    .filter(c -> c.getUserId().equals(i.getArgument(0)) && c.getChannelCode().equals(i.getArgument(1))).findFirst().orElse(null));
            when(configs.selectById(anyLong())).thenAnswer(i -> configRows.get(i.getArgument(0)));
            when(versions.selectById(anyLong())).thenAnswer(i -> versionRows.get(i.getArgument(0)));
            when(configs.insert(any(ChannelCredential.class))).thenAnswer(i -> { ChannelCredential c = i.getArgument(0); c.setId(ids.incrementAndGet()); configRows.put(c.getId(), c); return 1; });
            when(versions.insert(any(ChannelCredentialVersion.class))).thenAnswer(i -> { ChannelCredentialVersion v = i.getArgument(0); v.setId(ids.incrementAndGet()); versionRows.put(v.getId(), v); return 1; });
        }
    }
}
