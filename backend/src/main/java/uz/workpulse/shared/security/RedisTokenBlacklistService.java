package uz.workpulse.shared.security;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.security.token-blacklist.backend", havingValue = "redis")
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public RedisTokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void blacklist(String tokenHash, Duration ttl) {
        if (ttl.isZero() || ttl.isNegative()) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + tokenHash, "1", ttl);
    }

    @Override
    public boolean isBlacklisted(String tokenHash) {
        Boolean exists = redisTemplate.hasKey(KEY_PREFIX + tokenHash);
        return Boolean.TRUE.equals(exists);
    }
}
