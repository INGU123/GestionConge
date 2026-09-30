package com.fruvio.GestionConge.utilisateur.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "utilisateur")
public class Utilisateur implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String matricule;

    private String nom;

    private String prenom;

    @Column(unique = true, nullable = false)
    @Pattern(
        regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
        message = "Email invalide"
    )
    private String email;

    /*
     * Le mot de passe ne doit JAMAIS être renvoyé
     * dans les réponses JSON.
     */
    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;

    private Long service_id;

    private Long manager_id;

    @Column(nullable = false)
    private LocalDate date_embauche;

    @Builder.Default
    private boolean actif = true;

    @Column(nullable = false)
    private LocalDate date_creation;

    @Enumerated(EnumType.STRING)
    private Role role;

    /*
     * Ces champs sont également sensibles.
     */
    @JsonIgnore
    @Column(name = "reset_token")
    private String resetToken;

    @JsonIgnore
    @Column(name = "token_expiration")
    private LocalDateTime tokenExpiration;

    /*
     * Compatibilité avec les anciennes requêtes.
     */
    @JsonProperty(
        value = "motDePass",
        access = JsonProperty.Access.WRITE_ONLY
    )
    public void setMotDePass(String motDePass) {
        this.password = motDePass;
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {

        if (this.role == null) {
            return List.of();
        }

        return List.of(
            new SimpleGrantedAuthority(
                "ROLE_" + this.role.name()
            )
        );
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return matricule;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return actif;
    }
}