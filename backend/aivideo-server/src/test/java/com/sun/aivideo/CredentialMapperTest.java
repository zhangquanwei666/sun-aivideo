package com.sun.aivideo;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;

/** 实际解析生产 Mapper，校验版本绑定字段和清理 SQL 的动态分支。 */
class CredentialMapperTest {
    @Test void userQueriesWorkWithOnlyAccountFieldsAndStatusFilters() throws Exception {
        var config = new MybatisConfiguration();
        try (var stream = getClass().getResourceAsStream("/mapper/UserMapper.xml")) {
            assertNotNull(stream);
            new XMLMapperBuilder(stream, config, "UserMapper.xml", config.getSqlFragments()).parse();
        }
        var params = new HashMap<String, Object>();
        params.put("keyword", "alice"); params.put("status", "ENABLED");
        var query = config.getMappedStatement("com.sun.aivideo.dao.UserMapper.selectUserPage");
        var bound = query.getBoundSql(params);
        assertTrue(bound.getSql().contains("u.status = ?"));
        assertTrue(bound.getSql().contains("u.username LIKE"));
        assertEquals(java.util.List.of("keyword", "keyword", "status"), bound.getParameterMappings().stream()
                .map(org.apache.ibatis.mapping.ParameterMapping::getProperty).toList());
        params.put("keyword", null); params.put("status", null);
        assertFalse(query.getBoundSql(params).getSql().contains("u.status = ?"));
        for (String id : new String[]{"selectByUsername", "selectByIdForUpdate", "selectUserPage", "selectOverview"}) {
            String sql = config.getMappedStatement("com.sun.aivideo.dao.UserMapper." + id).getBoundSql(params).getSql().toLowerCase();
            assertFalse(sql.contains("member")); assertFalse(sql.contains("vip"));
        }
    }

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
