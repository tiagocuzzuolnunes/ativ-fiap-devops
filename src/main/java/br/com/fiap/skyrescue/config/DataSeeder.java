package br.com.fiap.skyrescue.config;

import br.com.fiap.skyrescue.domain.*;
import br.com.fiap.skyrescue.repository.DroneRepository;
import br.com.fiap.skyrescue.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "staging"})
public class DataSeeder implements CommandLineRunner {

    private final DroneRepository droneRepository;
    private final MissionRepository missionRepository;

    @Override
    public void run(String... args) {
        if (droneRepository.count() > 0) {
            return;
        }
        log.info("Populando base de dados com dados de exemplo (seed)...");

        Drone alpha = droneRepository.save(Drone.builder()
                .serialNumber("SR-ALPHA-001")
                .model("SkyRescue Alpha X")
                .status(DroneStatus.AVAILABLE)
                .batteryLevel(95)
                .lastLatitude(-23.5505)
                .lastLongitude(-46.6333)
                .build());

        droneRepository.save(Drone.builder()
                .serialNumber("SR-BRAVO-002")
                .model("SkyRescue Bravo Pro")
                .status(DroneStatus.CHARGING)
                .batteryLevel(40)
                .lastLatitude(-22.9068)
                .lastLongitude(-43.1729)
                .build());

        missionRepository.save(Mission.builder()
                .title("Resgate - Enchente Zona Leste")
                .description("Deteccao automatica de vitimas em area alagada")
                .disasterType(DisasterType.FLOOD)
                .latitude(-23.5489)
                .longitude(-46.4692)
                .status(MissionStatus.IN_PROGRESS)
                .drone(alpha)
                .build());

        log.info("Seed concluido: {} drones e {} missoes", droneRepository.count(), missionRepository.count());
    }
}
