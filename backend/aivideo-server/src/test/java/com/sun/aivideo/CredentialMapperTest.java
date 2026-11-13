package com.sun.aivideo;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;

/** 实际解析生产 Mapper，校验版本绑定字段和清理 SQL 的动态分支。 */
class CredentialMapperTest {
    @Test void mapperResolvesVersionBindingsAndNeverPurgesCurrentOrActiveVersions() throws Exception {
        var config = new MybatisConfiguration();
        for (String file : new String[]{"ChannelCredentialMapper.xml", "VideoTaskMapper.xml"}) {
            try (var stream = getClass().getResourceAsStream("/mapper/" + file)) {
                assertNotNull(stream);
                new XMLMapperBuilder(stream, config, file, config.getSqlFragments()).parse();
            }
        }
        var params = new HashMap<String, Object>(); params.put("id", 1L);
        var cleanup = config.getMappedStatement("com.sun.aivideo.dao.ChannelCredentialMapper.purgeUnusedSecrets");
        String sql = cleanup.getBoundSql(params).getSql();
        assertTrue(sql.contains("current_version_id")); assertTrue(sql.contains("v.id NOT IN"));
        assertTrue(sql.contains("llm_credential_version_id")); assertTrue(sql.contains("c.id = ?"));
        params.put("id", null);
        assertFalse(cleanup.getBoundSql(params).getSql().contains("c.id = ?"));
        var mappings = config.getResultMap("com.sun.aivideo.dao.VideoTaskMapper.BaseResultMap").getResultMappings();
        assertTrue(mappings.stream().anyMatch(m -> "credentialVersionId".equals(m.getProperty())));
        assertTrue(mappings.stream().anyMatch(m -> "llmCredentialVersionId".equals(m.getProperty())));
    }
}
