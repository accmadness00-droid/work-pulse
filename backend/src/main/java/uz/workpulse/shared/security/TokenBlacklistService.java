package uz.workpulse.shared.security;

import java.time.Duration;

public interface TokenBlacklistService {

    void blacklist(String tokenHash, Duration ttl);

    boolean isBlacklisted(String tokenHash);
}
