package it.odvsicilia.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseConfig Tests")
public class DatabaseConfigTest {

    @Test
    @DisplayName("DatabaseConfig should be instantiable")
    void testDatabaseConfigInstantiation() {
        assertDoesNotThrow(() -> {
            new DatabaseConfig();
        });
    }
}
