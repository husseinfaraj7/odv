package it.odvsicilia.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("prod")
public class ApplicationStartupProdProfileTest {

    @Test
    public void contextLoadsWithProdProfile() {
        // This test will pass if the application context loads successfully
        // with production profile without any BeanDefinitionStoreException
    }
}