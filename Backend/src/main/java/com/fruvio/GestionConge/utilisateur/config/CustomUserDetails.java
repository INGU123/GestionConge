package com.fruvio.GestionConge.utilisateur.config;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fruvio.GestionConge.utilisateur.entity.Role;
import com.fruvio.GestionConge.utilisateur.entity.Utilisateur;

public class CustomUserDetails implements UserDetails {

    private final Utilisateur utilisateur;

    public CustomUserDetails(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public Long getId() {
        return utilisateur != null ? utilisateur.getId() : null;
    }

    public Role getRole() {
        return utilisateur != null ? utilisateur.getRole() : null;
    }

    public Long getServiceId() {
        return utilisateur != null ? utilisateur.getService_id() : null;
    }

    public Long getManagerId() {
        return utilisateur != null ? utilisateur.getManager_id() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (utilisateur == null || utilisateur.getRole() == null) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name()));
    }

    @Override
    public String getPassword() {
        return utilisateur != null ? utilisateur.getPassword() : null;
    }

    @Override
    public String getUsername() {
        return utilisateur != null ? utilisateur.getMatricule() : null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return utilisateur != null && utilisateur.isActif() && !utilisateur.isArchived();
    }
}