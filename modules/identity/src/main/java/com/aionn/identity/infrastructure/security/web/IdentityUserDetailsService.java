package com.aionn.identity.infrastructure.security.web;

import com.aionn.identity.domain.valueobject.UserStatus;
import com.aionn.identity.infrastructure.persistence.entity.UserEntity;
import com.aionn.identity.infrastructure.persistence.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IdentityUserDetailsService implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String identity) throws UsernameNotFoundException {
                UserEntity user = userRepository.findByEmailIgnoreCase(identity)
                                .or(() -> userRepository.findByPhone(identity))
                                .or(() -> userRepository.findByUsernameIgnoreCase(identity))
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                Set<GrantedAuthority> authorities = user.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                .collect(Collectors.toSet());

                boolean accountLocked = user.getLockedUntil() != null
                                && user.getLockedUntil().isAfter(LocalDateTime.now());
                boolean enabled = user.getStatus() == UserStatus.ACTIVE;

                return User.builder()
                                .username(user.getUserId())
                                .password(user.getPasswordHash() == null ? "" : user.getPasswordHash())
                                .authorities(authorities)
                                .accountLocked(accountLocked)
                                .disabled(!enabled)
                                .build();
        }
}
