package bdd.skyrescue;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BddScenarioContext {

    private Long droneId;
    private Long missionId;
    private String lastResponseBody;
    private int lastHttpStatus;
}
