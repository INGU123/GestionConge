package com.fruvio.GestionConge.utilisateur.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.cors(Customizer.withDefaults()).csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // Requêtes préflight CORS et authentification publique
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/utilisateur/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/utilisateur/forgot-password").permitAll()
                        .requestMatchers("/auth/**").permitAll().requestMatchers("/error").permitAll()

                        // Endpoints réservés à l'ADMIN
                        .requestMatchers(HttpMethod.POST, "/utilisateur/create").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/solde/initialiser/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/solde/ajuster/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/type-conge/create").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/type-conge/update/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/type-conge/delete/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/services/create").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/services/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/historiques/creer").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/notification/creer").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/jours-feries/create").hasRole("ADMIN")

                        // Endpoints réservés à ADMIN et MANAGER
                        .requestMatchers("/utilisateur/all").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/conge/*/traiter").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/conge/manager/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/conge/en-attente").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/conge/all").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/solde", "/solde/all").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/historiques", "/historiques/all")
                        .hasAnyRole("ADMIN", "MANAGER")

                        // Endpoints accessibles aux utilisateurs connectés (avec vérifications de
                        // propriété/périmètre)
                        .requestMatchers(HttpMethod.GET, "/utilisateur/{id}").hasAnyRole("ADMIN", "MANAGER", "EMPLOYE")
                        .requestMatchers(HttpMethod.PUT, "/utilisateur/update")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYE")
                        .requestMatchers(HttpMethod.PUT, "/utilisateur/change-password")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYE")
                        .requestMatchers(HttpMethod.POST, "/conge/demander/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYE").requestMatchers(HttpMethod.PUT, "/conge/*/annuler")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYE")
                        .requestMatchers(HttpMethod.GET, "/conge/mes-demandes/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYE")
                        .requestMatchers(HttpMethod.GET, "/solde/utilisateur/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYE").requestMatchers(HttpMethod.GET, "/type-conge/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYE").requestMatchers(HttpMethod.GET, "/services/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYE").requestMatchers("/jours-feries/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYE").requestMatchers("/notification/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYE")
                        .requestMatchers(HttpMethod.GET, "/historiques/utilisateur/**")
                        .hasAnyRole("ADMIN", "MANAGER", "EMPLOYE")

                        // Toute autre requête nécessite une authentification
                        .anyRequest().authenticated())

                // Architecture stateless sans session serveur
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Filtre JWT placé avant le filtre standard d'authentification
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {

        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://127.0.0.1:3000"));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        configuration
                .setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));

        configuration.setExposedHeaders(List.of("Authorization"));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}