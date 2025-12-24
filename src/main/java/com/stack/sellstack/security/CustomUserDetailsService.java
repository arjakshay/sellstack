package com.stack.sellstack.security;

import com.stack.sellstack.model.entity.Seller;
import com.stack.sellstack.service.SellerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final SellerService sellerService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("CustomUserDetailsService loading user by username: {}", username);

        // sellerService.findByUsername() returns Seller or null
        Seller seller = sellerService.findByUsername(username);

        if (seller == null) {
            log.error("Seller not found for username: {}", username);
            throw new UsernameNotFoundException("User not found: " + username);
        }

        log.debug("Found seller: {} (ID: {})", seller.getPhone(), seller.getId());

        // Check if seller has a role field
        if (seller.getRole() == null) {
            log.warn("Seller {} has null role! Using default ROLE_SELLER", seller.getPhone());
        }

        String roleName = (seller.getRole() != null) ?
                seller.getRole().name() : "SELLER";

        log.debug("Creating UserDetails with role: {}", roleName);

        return User.builder()
                .username(seller.getPhone()) // Use phone as username to match JWT subject
                .password(seller.getPasswordHash())
                .authorities(Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + roleName)
                ))
                .accountExpired(false)
                .accountLocked(!seller.isEnabled())
                .credentialsExpired(false)
                .disabled(!seller.isEnabled())
                .build();
    }
}