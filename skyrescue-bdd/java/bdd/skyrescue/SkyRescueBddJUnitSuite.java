package bdd.skyrescue;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

import org.junit.jupiter.api.Tag;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit 5 Platform entry point for Cucumber BDD scenarios under {@code skyrescue-bdd/features}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "bdd.skyrescue")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
@Tag("bdd")
public class SkyRescueBddJUnitSuite {
}
