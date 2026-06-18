package uz.workpulse.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import uz.workpulse.device.application.DeviceApiKeyService;
import uz.workpulse.device.domain.Device;
import uz.workpulse.shared.exception.BusinessException;
import uz.workpulse.shared.exception.ErrorCode;

@Component
public class DeviceApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER_DEVICE_SERIAL = "X-Device-Serial";
    public static final String HEADER_DEVICE_API_KEY = "X-Device-Api-Key";
    public static final String DEVICE_PUSH_AUTHORITY = "DEVICE_PUSH";

    private final DeviceApiKeyService deviceApiKeyService;

    public DeviceApiKeyAuthenticationFilter(DeviceApiKeyService deviceApiKeyService) {
        this.deviceApiKeyService = deviceApiKeyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !"/api/v1/device-events".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String serial = request.getHeader(HEADER_DEVICE_SERIAL);
        String apiKey = request.getHeader(HEADER_DEVICE_API_KEY);
        if (!StringUtils.hasText(serial) || !StringUtils.hasText(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Device device = deviceApiKeyService.authenticateDevice(serial, apiKey);
            DevicePushPrincipal principal = new DevicePushPrincipal(device.getId(), device.getBranchId(), device.getSerialNumber());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority(DEVICE_PUSH_AUTHORITY))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BusinessException ex) {
            SecurityContextHolder.clearContext();
            response.setStatus(ex.getErrorCode().getStatus().value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"success":false,"data":null,"message":"%s","errors":null,"timestamp":"%s"}
                    """.formatted(ex.getMessage(), java.time.Instant.now()));
        }
    }
}
