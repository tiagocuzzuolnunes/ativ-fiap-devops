package bdd.skyrescue;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@CucumberContextConfiguration
@SpringBootTest(classes = br.com.fiap.skyrescue.SkyRescueApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(BddTestGlueConfiguration.class)
public class CucumberSpringConfiguration {
}
