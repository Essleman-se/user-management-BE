package micronet.user.config;

import micronet.user.filter.JwtAuthenticationFilter;
import micronet.user.handler.OAuth2AuthenticationSuccessHandler;
import micronet.user.oauth2.OAuth2ReturnUrlCookieFilter;
import micronet.user.service.OAuth2UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private OAuth2UserService oAuth2UserService;

    @Autowired
    private OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private OAuth2ReturnUrlCookieFilter oAuth2ReturnUrlCookieFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(oAuth2ReturnUrlCookieFilter, OAuth2AuthorizationRequestRedirectFilter.class)
            .cors(cors -> cors.configurationSource(corsConfigurationSource)) // Enable CORS
            .csrf(csrf -> csrf.disable()) // Disable CSRF for REST API
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // Allow authentication endpoints
                .requestMatchers("/api/oauth2/**").permitAll() // Allow OAuth2 React API endpoints
                .requestMatchers("/oauth2/**").permitAll() // Allow OAuth2 endpoints
                .requestMatchers("/login", "/login/**").permitAll() // Allow OAuth2 login
                .requestMatchers("/h2-console/**").permitAll() // Allow H2 console access
                .requestMatchers("/api/users/hello").permitAll() // Allow hello endpoint
                .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN") // Require USER or ADMIN role
                .requestMatchers("/api/users/**/delete").hasRole("ADMIN") // Only ADMIN can delete
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Stateless session for REST API
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(oAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // Add JWT filter

        // Allow H2 console frames
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

