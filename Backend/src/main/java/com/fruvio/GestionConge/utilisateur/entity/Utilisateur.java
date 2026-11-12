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

    @Column(unique = true, length = 50)
    private String matricule;

    private String nom;

    private String prenom;

    @Column(name = "numero_cin", unique = true, length = 30)
    private String numeroCin;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(length = 30)
    private String telephone;

    @Column(length = 500)
    private String adresse;

    @Column(length = 100)
    private String fonction;

    @Column(name = "type_contrat", length = 80)
    private String typeContrat;

    @Column(unique = true, nullable = true)
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

    // Archivage logique : les références historiques continuent de pointer vers ce compte.
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Transitoire : transmis uniquement dans la réponse de création à l'ADMIN.
    @Transient
    @JsonIgnore
    private String generatedPasswordForAdmin;

    @Column(name = "deleted_by")
    private Long deletedBy;

    @Column(name = "deletion_reason", length = 1000)
    private String deletionReason;

    @Column(name = "actif_avant_archivage")
    private Boolean actifAvantArchivage;

    @Column(name = "archive_email_sent_at")
    private LocalDateTime archiveEmailSentAt;

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

    @Transient
    @JsonProperty("archived")
    public boolean isArchived() {
        return deletedAt != null;
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return actif && !isArchived();
    }
}