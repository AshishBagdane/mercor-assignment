package com.mercor.assignment.scd.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class CacheConfiguration {

  @Bean
  RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    final ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.enable(JsonGenerator.Feature.IGNORE_UNKNOWN);
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);

    final RedisSerializer<Object> serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

    final RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
        .disableCachingNullValues()
        .entryTtl(Duration.ofDays(1))
        .enableTimeToIdle();



    // Create different configs for different cache types
    RedisCacheConfiguration latestVersionConfig = config.entryTtl(Duration.ofHours(2));
    RedisCacheConfiguration criteriaQueriesConfig = config.entryTtl(Duration.ofMinutes(30));
    RedisCacheConfiguration aggregateResultsConfig = config.entryTtl(Duration.ofMinutes(15));
    RedisCacheConfiguration versionHistoryConfig = config.entryTtl(Duration.ofDays(1));

    Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

    // Latest version caches
    cacheConfigurations.put("job:latest", latestVersionConfig);
    cacheConfigurations.put("timelog:latest", latestVersionConfig);
    cacheConfigurations.put("payment_line_item:latest", latestVersionConfig);

    // Criteria query caches
    cacheConfigurations.put("job:activeByCompany", criteriaQueriesConfig);
    cacheConfigurations.put("job:activeByContractor", criteriaQueriesConfig);

    // Aggregate caches
    cacheConfigurations.put("payment_line_item:totalForContractor", aggregateResultsConfig);

    // Version history caches
    cacheConfigurations.put("job:history", versionHistoryConfig);
    cacheConfigurations.put("timelog:history", versionHistoryConfig);
    cacheConfigurations.put("payment_line_item:history", versionHistoryConfig);

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(config)
        .withInitialCacheConfigurations(cacheConfigurations)
        .build();
  }
}
