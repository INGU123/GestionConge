package com.fruvio.GestionConge.utilisateur.migration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Migration idempotente des anciens comptes MANAGER.
 * Les comptes et leurs données sont conservés ; seul leur rôle devient EMPLOYE.
 */
@Component
public class RoleObsoleteMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public RoleObsoleteMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.update(
                "UPDATE utilisateur SET role = 'EMPLOYE' WHERE UPPER(role) IN ('MANAGER', 'ROLE_MANAGER')");
        jdbcTemplate.execute("ALTER TABLE utilisateur DROP COLUMN IF EXISTS manager_id");
    }
}
