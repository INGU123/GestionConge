package com.fruvio.GestionConge.utilisateur.config;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/utilisateur/login") || path.startsWith("/utilisateur/forgot-password")
                || path.startsWith("/utilisateur/reset-password") || path.startsWith("/auth/")
                || path.startsWith("/error");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();

            try {
                if (jwtUtil.isTokenValid(token)) {
                    String matricule = jwtUtil.extractMatricule(token);

                    if (matricule != null && !matricule.trim().isEmpty()
                            && SecurityContextHolder.getContext().getAuthentication() == null) {

                        UserDetails userDetails = userDetailsService.loadUserByUsername(matricule.trim());

                        // Vérification critique : compte actif et non verrouillé
                        if (userDetails != null && userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        } else {
                            logger.warn("Tentative d'accès rejetée : compte inactif ou désactivé pour le matricule "
                                    + matricule);
                        }
                    }
                }
            } catch (UsernameNotFoundException e) {
                logger.warn("Utilisateur supprimé ou introuvable pour le token fourni");
            } catch (Exception e) {
                logger.warn("Échec d'authentification par jeton JWT");
            }
        }

        filterChain.doFilter(request, response);
    }
}
