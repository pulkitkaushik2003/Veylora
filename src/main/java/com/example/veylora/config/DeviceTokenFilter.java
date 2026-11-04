package com.example.veylora.config;

import com.example.veylora.Model.Device;
import com.example.veylora.Repository.DeviceRepository;
import com.example.veylora.util.Hashing;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** Authenticates calls to /api/v1/device-agent/** using the X-Device-Token header. */
public class DeviceTokenFilter extends OncePerRequestFilter {

    private final DeviceRepository deviceRepository;

    public DeviceTokenFilter(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/device-agent/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = request.getHeader("X-Device-Token");
        if (token != null && !token.isBlank()) {
            deviceRepository.findByTokenHash(Hashing.sha256(token.trim()))
                    .filter(d -> d.getStatus() != Device.DeviceStatus.DISABLED)
                    .ifPresent(device -> {
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                                device.getId(), null,
                                List.of(new SimpleGrantedAuthority("ROLE_DEVICE"))));
                        SecurityContextHolder.setContext(context);
                    });
        }
        chain.doFilter(request, response);
    }
}