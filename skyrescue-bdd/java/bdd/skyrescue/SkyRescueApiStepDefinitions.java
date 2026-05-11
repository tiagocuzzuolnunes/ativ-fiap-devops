package bdd.skyrescue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
public class SkyRescueApiStepDefinitions {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final BddScenarioContext ctx;

    @Given("a registered drone is available for rescue operations")
    public void aRegisteredDroneIsAvailable() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("serialNumber", "SR-BDD-" + System.nanoTime());
        payload.put("model", "SkyRescue BDD Mock");
        payload.put("batteryLevel", 100);
        payload.put("lastLatitude", -23.5505);
        payload.put("lastLongitude", -46.6333);

        MvcResult result = mockMvc.perform(post("/api/v1/drones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        ctx.setDroneId(body.get("id").asLong());
    }

    @When("I create a rescue mission linked to that drone for disaster type {string}")
    public void iCreateARescueMissionLinkedToThatDrone(String disasterType) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "BDD rescue — coordinated response");
        payload.put("description", "Automated BDD mission with drone assignment");
        payload.put("disasterType", disasterType);
        payload.put("latitude", -23.5489);
        payload.put("longitude", -46.4692);
        payload.put("droneId", ctx.getDroneId());

        MvcResult result = mockMvc.perform(post("/api/v1/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();

        ctx.setLastHttpStatus(result.getResponse().getStatus());
        ctx.setLastResponseBody(result.getResponse().getContentAsString());
        if (result.getResponse().getStatus() == 201) {
            JsonNode body = objectMapper.readTree(ctx.getLastResponseBody());
            ctx.setMissionId(body.get("id").asLong());
        }
    }

    @Then("the mission is created with HTTP status {int}")
    public void theMissionIsCreatedWithHttpStatus(int expected) {
        assertThat(ctx.getLastHttpStatus()).isEqualTo(expected);
    }

    @And("the mission has status {string}")
    public void theMissionHasStatus(String expectedStatus) throws Exception {
        mockMvc.perform(get("/api/v1/missions/" + ctx.getMissionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(expectedStatus));
    }

    @And("the mission lists the assigned drone id")
    public void theMissionListsTheAssignedDroneId() throws Exception {
        mockMvc.perform(get("/api/v1/missions/" + ctx.getMissionId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.droneId").value(ctx.getDroneId().intValue()));
    }

    @Given("no mission exists with id {long}")
    public void noMissionExistsWithId(long ignoredMissionId) {
        // Governance precondition: mission id is not used in this scenario's data setup.
    }

    @When("I attempt to register a victim on mission {long}")
    public void iAttemptToRegisterAVictimOnMission(long missionId) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("identification", "BDD-GOV-001");
        payload.put("condition", "INJURED");
        payload.put("latitude", -23.481);
        payload.put("longitude", -45.921);
        payload.put("detectionConfidence", 0.91);

        MvcResult result = mockMvc.perform(post("/api/v1/missions/" + missionId + "/victims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andReturn();

        ctx.setLastHttpStatus(result.getResponse().getStatus());
        ctx.setLastResponseBody(result.getResponse().getContentAsString());
    }

    @Then("the API responds with HTTP status {int}")
    public void theApiRespondsWithHttpStatus(int expected) {
        assertThat(ctx.getLastHttpStatus()).isEqualTo(expected);
    }

    @And("the error payload indicates the mission was not found")
    public void theErrorPayloadIndicatesTheMissionWasNotFound() throws Exception {
        JsonNode root = objectMapper.readTree(ctx.getLastResponseBody());
        assertThat(root.path("status").asInt()).isEqualTo(404);
        assertThat(root.path("message").asText()).containsIgnoringCase("Missao");
        assertThat(root.path("message").asText()).containsIgnoringCase("999999");
    }

    @Given("a rescue mission exists without an assigned drone")
    public void aRescueMissionExistsWithoutAnAssignedDrone() throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", "BDD tracking — unassigned mission");
        payload.put("description", "Mission for ESG status tracking scenario");
        payload.put("disasterType", "EARTHQUAKE");
        payload.put("latitude", -22.9068);
        payload.put("longitude", -43.1729);

        MvcResult result = mockMvc.perform(post("/api/v1/missions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        ctx.setMissionId(body.get("id").asLong());
    }

    @When("I update the mission status to {string}")
    public void iUpdateTheMissionStatusTo(String status) throws Exception {
        MvcResult result = mockMvc.perform(patch("/api/v1/missions/" + ctx.getMissionId() + "/status")
                        .queryParam("status", status))
                .andReturn();
        ctx.setLastHttpStatus(result.getResponse().getStatus());
        ctx.setLastResponseBody(result.getResponse().getContentAsString());
    }

    @Then("the mission status returned is {string}")
    public void theMissionStatusReturnedIs(String expected) throws Exception {
        assertThat(ctx.getLastHttpStatus()).isEqualTo(200);
        JsonNode root = objectMapper.readTree(ctx.getLastResponseBody());
        assertThat(root.path("status").asText()).isEqualTo(expected);
    }
}
