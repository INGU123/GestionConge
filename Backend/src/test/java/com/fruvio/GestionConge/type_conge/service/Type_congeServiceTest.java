package com.fruvio.GestionConge.type_conge.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fruvio.GestionConge.type_conge.entity.Type_conge;
import com.fruvio.GestionConge.type_conge.repository.Type_congeRepository;

@ExtendWith(MockitoExtension.class)
class Type_congeServiceTest {

    @Mock
    private Type_congeRepository repository;

    @InjectMocks
    private Type_congeService service;

    @Test
    void rejectsDuplicateCodeIgnoringCaseAndWhitespace() {
        Type_conge type = Type_conge.builder()
                .code(" cp ").libelle("Congés payés").build();
        when(repository.existsByCodeIgnoreCase("CP")).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> service.save(type));

        assertEquals(true, error.getMessage().contains("Type existant"));
    }

    @Test
    void rejectsDuplicateLabelIgnoringCase() {
        Type_conge type = Type_conge.builder()
                .code("MAL").libelle("congés payés").build();
        when(repository.existsByCodeIgnoreCase("MAL")).thenReturn(false);
        when(repository.existsByLibelleIgnoreCase("congés payés")).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> service.save(type));

        assertEquals(true, error.getMessage().contains("Type existant"));
    }

    @Test
    void normalizesCodeAndLabelBeforeSaving() {
        Type_conge type = Type_conge.builder()
                .code(" cp ").libelle(" Congés payés ").build();
        when(repository.existsByCodeIgnoreCase("CP")).thenReturn(false);
        when(repository.existsByLibelleIgnoreCase("Congés payés")).thenReturn(false);
        when(repository.save(type)).thenReturn(type);

        Type_conge saved = service.save(type);

        assertEquals("CP", saved.getCode());
        assertEquals("Congés payés", saved.getLibelle());
    }
}
