package uz.workpulse.shared.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.security.token-blacklist.backend", havingValue = "memory", matchIfMissing = true)
public class InMemoryTokenBlacklistService implements TokenBlacklistService {

    private final Map<String, Instant> entries = new ConcurrentHashMap<>();

    @Override
    public void blacklist(String tokenHash, Duration ttl) {
        entries.put(tokenHash, Instant.now().plus(ttl));
    }

    @Override
    public boolean isBlacklisted(String tokenHash) {
        Instant expiresAt = entries.get(tokenHash);
        if (expiresAt == null) {
            return false;
        }
        if (Instant.now().isAfter(expiresAt)) {
            entries.remove(tokenHash);
            return false;
        }
        return true;
    }

    @Scheduled(fixedDelayString = "${app.security.token-blacklist.cleanup-ms:300000}")
    void cleanupExpired() {
        Instant now = Instant.now();
        entries.entrySet().removeIf(entry -> now.isAfter(entry.getValue()));
    }
}
