package com.stack.sellstack.controller.auth;

import com.stack.sellstack.exception.AuthenticationException;
import com.stack.sellstack.exception.BusinessException;
import com.stack.sellstack.exception.OTPException;
import com.stack.sellstack.exception.ValidationException;
import com.stack.sellstack.model.dto.request.AuthRequest;
import com.stack.sellstack.model.dto.response.ApiResponse;
import com.stack.sellstack.model.dto.response.AuthResponse;
import com.stack.sellstack.model.dto.response.SellerResponse;
import com.stack.sellstack.model.entity.Seller;
import com.stack.sellstack.model.enums.OTPType;
import com.stack.sellstack.security.JwtTokenProvider;
import com.stack.sellstack.security.SecurityUtils;
import com.stack.sellstack.service.DeviceService;
import com.stack.sellstack.service.OTPService;
import com.stack.sellstack.service.SecurityAuditService;
import com.stack.sellstack.service.SellerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Authentication", description = "Authentication APIs for sellers")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final SellerService sellerService;
    private final OTPService otpService;
    private final DeviceService deviceService;
    private final SecurityAuditService securityAuditService;

    @PostMapping("/register/initiate")
    @Operation(summary = "Initiate seller registration with OTP")
    public ResponseEntity<ApiResponse<Void>> initiateRegistration(
            @Valid @RequestBody AuthRequest.InitiateRegistrationRequest request,
            HttpServletRequest httpRequest) {

        // Validate phone number format
        if (!isValidIndianPhoneNumber(request.getPhone())) {
            throw new ValidationException("Invalid Indian phone number format");
        }

        // Check if phone/email already registered
        if (sellerService.existsByPhone(request.getPhone())) {
            throw new BusinessException("Phone number already registered");
        }

        if (request.getEmail() != null && sellerService.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered");
        }

        // Generate and send OTP - Use OTPType enum
        otpService.generateAndSendOtp(
                request.getPhone(),
                request.getEmail(),
                OTPType.REGISTRATION  // Use enum instead of string
        );

        securityAuditService.logRegistrationInitiated(
                request.getPhone(),
                SecurityUtils.getClientIP(httpRequest)
        );

        return ResponseEntity.ok(ApiResponse.success(
                null,
                "OTP sent successfully to " + maskPhone(request.getPhone())
        ));
    }

    @PostMapping("/register/verify")
    @Operation(summary = "Verify OTP and complete registration")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyAndRegister(
            @Valid @RequestBody AuthRequest.VerifyRegistrationRequest request,
            HttpServletRequest httpRequest) {

        // Verify OTP - Use OTPType enum
        boolean isOtpValid = otpService.verifyOtp(
                request.getPhone(),
                request.getOtpCode(),
                OTPType.REGISTRATION
        );

        if (!isOtpValid) {
            throw new OTPException("Invalid or expired OTP");
        }

        // Create seller
        Seller seller = sellerService.createSeller(request);

        // Generate device ID
        String deviceId = deviceService.registerDevice(httpRequest);

        // Create authentication WITHOUT using authenticationManager to avoid recursion
        List<GrantedAuthority> authorities = Arrays.asList(
                new SimpleGrantedAuthority("ROLE_SELLER")
        );

        UserDetails userDetails = new User(
                seller.getPhone(),
                seller.getPasswordHash(), // Should be encoded password
                true, true, true, true,
                authorities
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                authorities
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate tokens
        String accessToken = jwtTokenProvider.createAccessToken(authentication, deviceId);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication, deviceId);

        // Create session - Update to match DeviceService method
        deviceService.createSession(seller.getId(), deviceId, refreshToken, httpRequest);

        // Security audit
        securityAuditService.logRegistrationCompleted(
                seller.getId(),
                SecurityUtils.getClientIP(httpRequest)
        );

        // Build response - Get expiration from JWT properties
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenValidityInSeconds()) // Use this method
                .seller(SellerResponse.fromEntity(seller))
                .build();

        // Set refresh token as HTTP-only cookie
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true) // HTTPS only in production
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success(authResponse, "Registration successful"));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with phone/email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody AuthRequest.LoginRequest request,
            HttpServletRequest httpRequest) {

        // Rate limiting for login attempts
        securityAuditService.checkLoginAttempts(
                request.getUsername(),
                SecurityUtils.getClientIP(httpRequest)
        );

        try {
            // Get seller details first
            Seller seller = sellerService.findByUsername(request.getUsername());

            if (seller == null) {
                securityAuditService.logLoginFailed(
                        request.getUsername(),
                        SecurityUtils.getClientIP(httpRequest),
                        "User not found"
                );
                throw new AuthenticationException("Invalid credentials");
            }

            // Verify password manually
            if (!sellerService.verifyPassword(seller, request.getPassword())) {
                securityAuditService.logLoginFailed(
                        request.getUsername(),
                        SecurityUtils.getClientIP(httpRequest),
                        "Invalid password"
                );
                throw new AuthenticationException("Invalid credentials");
            }

            // Create authentication WITHOUT using authenticationManager
            List<GrantedAuthority> authorities = seller.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                    .collect(Collectors.toList());

            UserDetails userDetails = new User(
                    seller.getPhone(),
                    seller.getPasswordHash(),
                    authorities
            );

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate device ID
            String deviceId = deviceService.registerDevice(httpRequest);

            // Generate tokens
            String accessToken = jwtTokenProvider.createAccessToken(authentication, deviceId);
            String refreshToken = jwtTokenProvider.createRefreshToken(authentication, deviceId);

            // Create session
            deviceService.createSession(seller.getId(), deviceId, refreshToken, httpRequest);

            // Security audit
            securityAuditService.logLoginSuccess(
                    seller.getId(),
                    SecurityUtils.getClientIP(httpRequest)
            );

            // Build response
            AuthResponse authResponse = AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getAccessTokenValidityInSeconds())
                    .seller(SellerResponse.fromEntity(seller))
                    .build();

            // Set refresh token as HTTP-only cookie
            ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .path("/api/v1/auth/refresh")
                    .maxAge(Duration.ofDays(7))
                    .sameSite("Strict")
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                    .body(ApiResponse.success(authResponse, "Login successful"));

        } catch (AuthenticationException e) {
            throw e; // Re-throw our custom exceptions
        } catch (Exception e) {
            securityAuditService.logLoginFailed(
                    request.getUsername(),
                    SecurityUtils.getClientIP(httpRequest),
                    e.getMessage()
            );
            throw new AuthenticationException("Invalid credentials");
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse.RefreshTokenResponse>> refreshToken(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenCookie,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshTokenHeader,
            HttpServletRequest httpRequest) {

        String refreshToken = refreshTokenCookie != null ? refreshTokenCookie : refreshTokenHeader;

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new AuthenticationException("Refresh token is required");
        }

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AuthenticationException("Invalid refresh token");
        }

        // Check token type
        if (!"REFRESH".equals(jwtTokenProvider.getTokenType(refreshToken))) {
            throw new AuthenticationException("Invalid token type");
        }

        // Get authentication from refresh token
        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);
        String deviceId = jwtTokenProvider.getDeviceIdFromToken(refreshToken);

        // Verify session is still active
        if (!deviceService.isSessionValid(authentication.getName(), deviceId, refreshToken)) {
            throw new AuthenticationException("Session expired or invalid");
        }

        // Generate new access token
        String newAccessToken = jwtTokenProvider.createAccessToken(authentication, deviceId);

        // Security audit
        securityAuditService.logTokenRefreshed(
                authentication.getName(),
                SecurityUtils.getClientIP(httpRequest)
        );

        AuthResponse.RefreshTokenResponse response = AuthResponse.RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(900L) // 15 minutes in seconds
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate session",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String deviceId = jwtTokenProvider.getDeviceIdFromToken(
                    extractTokenFromHeader(httpRequest)
            );

            deviceService.invalidateSession(authentication.getName(), deviceId);

            securityAuditService.logLogout(
                    authentication.getName(),
                    SecurityUtils.getClientIP(httpRequest)
            );
        }

        // Clear refresh token cookie
        ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/api/v1/auth/refresh")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
                .body(ApiResponse.success(null, "Logged out successfully"));
    }

    @PostMapping("/forgot-password/initiate")
    @Operation(summary = "Initiate password reset with OTP")
    public ResponseEntity<ApiResponse<Void>> initiatePasswordReset(
            @Valid @RequestBody AuthRequest.ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        // Check if user exists
        if (!sellerService.existsByPhone(request.getPhone())) {
            // Don't reveal that user doesn't exist (security best practice)
            return ResponseEntity.ok(ApiResponse.success(
                    null,
                    "If an account exists with this phone number, OTP will be sent"
            ));
        }

        // Generate and send OTP - Use OTPType enum
        otpService.generateAndSendOtp(
                request.getPhone(),
                null,
                OTPType.PASSWORD_RESET  // Use enum instead of string
        );

        securityAuditService.logPasswordResetInitiated(
                request.getPhone(),
                SecurityUtils.getClientIP(httpRequest)
        );

        return ResponseEntity.ok(ApiResponse.success(
                null,
                "OTP sent successfully to " + maskPhone(request.getPhone())
        ));
    }

    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Reset password with OTP verification")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody AuthRequest.ResetPasswordRequest request,
            HttpServletRequest httpRequest) {

        // Verify OTP - Use OTPType enum
        boolean isOtpValid = otpService.verifyOtp(
                request.getPhone(),
                request.getOtpCode(),
                OTPType.PASSWORD_RESET  // Use enum instead of string
        );

        if (!isOtpValid) {
            throw new OTPException("Invalid or expired OTP");
        }

        // Reset password
        sellerService.resetPassword(request.getPhone(), request.getNewPassword());

        // Invalidate all sessions for security
        sellerService.invalidateAllSessions(request.getPhone());

        securityAuditService.logPasswordResetCompleted(
                request.getPhone(),
                SecurityUtils.getClientIP(httpRequest)
        );

        return ResponseEntity.ok(ApiResponse.success(
                null,
                "Password reset successfully. Please login with new password."
        ));
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isValidIndianPhoneNumber(String phone) {
        // Indian phone number validation: +91 or 0 followed by 10 digits
        return phone != null && phone.matches("^(\\+91|0)?[6789]\\d{9}$");
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return phone;
        return phone.substring(0, phone.length() - 4) + "XXXX";
    }
}