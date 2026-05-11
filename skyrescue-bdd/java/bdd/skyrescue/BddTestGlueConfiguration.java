package bdd.skyrescue;

import io.cucumber.spring.ScenarioScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class BddTestGlueConfiguration {

    @Bean
    @ScenarioScope
    public BddScenarioContext bddScenarioContext() {
        return new BddScenarioContext();
    }
}
