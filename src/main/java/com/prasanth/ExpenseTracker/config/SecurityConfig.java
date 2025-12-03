package com.prasanth.ExpenseTracker.config;

import com.prasanth.ExpenseTracker.filters.JwtRequestFilter;
import com.prasanth.ExpenseTracker.model.User;
import com.prasanth.ExpenseTracker.repository.UserRepository;
import com.prasanth.ExpenseTracker.service.MyUserDetailsService;
import com.prasanth.ExpenseTracker.util.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final MyUserDetailsService myUserDetailsService;
    private final JwtRequestFilter jwtRequestFilter;

    // Using Constructor Injection instead of @Autowired fields
    public SecurityConfig(JwtUtil jwtUtil, MyUserDetailsService myUserDetailsService, JwtRequestFilter jwtRequestFilter) {
        this.jwtUtil = jwtUtil;
        this.myUserDetailsService = myUserDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationSuccessHandler oAuth2SuccessHandler) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/swagger-ui.html", "/swagger-ui/**", "/api-docs/**", "/login/oauth2/code/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return (request, response, authentication) -> {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getAttribute("email");

            if (email == null) {
                // It's possible the email is not public. Try to get it from a different attribute if needed.
                // For GitHub, the 'login' attribute is always present.
                String login = oAuth2User.getAttribute("login");
                // You might want to handle this case differently, e.g., by asking the user for an email.
                // For now, we'll create a placeholder email if the primary one is missing.
                email = login + "@github.user.com";
            }

            final String finalEmail = email;
            User user = userRepository.findByEmail(finalEmail)
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(finalEmail);
                        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                        String name = oAuth2User.getAttribute("name");
                        newUser.setName(name != null ? name : oAuth2User.getAttribute("login"));
                        return userRepository.save(newUser);
                    });

            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    user.getEmail(),
                    user.getPassword(),
                    new ArrayList<>()
            );

            // Use the jwtUtil field from the SecurityConfig instance
            final String jwt = this.jwtUtil.generateToken(userDetails);

            String frontendUrl = "http://localhost:3000/oauth-success";
            String redirectUrl = frontendUrl + "?token=" + jwt;
            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(myUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
