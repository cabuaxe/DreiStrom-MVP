package de.dreistrom.common.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class AppUserDetailsServiceTest {

    @Autowired
    private AppUserDetailsService service;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails() {
        appUserRepository.deleteAll();
        appUserRepository.save(new AppUser(
                "test@dreistrom.de",
                passwordEncoder.encode("pass"),
                "Test User"));

        UserDetails details = service.loadUserByUsername("test@dreistrom.de");

        assertThat(details.getUsername()).isEqualTo("test@dreistrom.de");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).hasSize(1);
    }

    @Test
    void loadUserByUsername_unknownUser_throwsUsernameNotFound() {
        assertThatThrownBy(() -> service.loadUserByUsername("unknown@dreistrom.de"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown@dreistrom.de");
    }
}
