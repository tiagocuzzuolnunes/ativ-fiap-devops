package bdd.skyrescue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract-style check: mission JSON returned by the API must conform to
 * {@code classpath:schemas/mission-response.schema.json} (shipped under {@code skyrescue-bdd/schemas}).
 */
@SpringBootTest(classes = br.com.fiap.skyrescue.SkyRescueApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("bdd")
class MissionResponseContractTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void getMissionResponseMatchesJsonSchema() throws Exception {
        Map<String, Object> createPayload = new LinkedHashMap<>();
        createPayload.put("title", "Contract test mission");
        createPayload.put("description", "Schema validation");
        createPayload.put("disasterType", "FLOOD");
        createPayload.put("latitude", -23.55);
        createPayload.put("longitude", -46.63);

        MvcResult created = mockMvc.perform(post("/api/v1/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsString());
        long missionId = createdBody.get("id").asLong();

        MvcResult fetched = mockMvc.perform(get("/api/v1/missions/" + missionId))
                .andExpect(status().isOk())
                .andReturn();

        String json = fetched.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        try (InputStream schemaStream = new ClassPathResource("schemas/mission-response.schema.json").getInputStream()) {
            JsonSchema schema = factory.getSchema(schemaStream);
            Set<ValidationMessage> errors = schema.validate(node);
            assertThat(errors).as("JSON Schema violations: %s", errors).isEmpty();
        }
    }
}
