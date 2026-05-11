package br.com.fiap.skyrescue.repository;

import br.com.fiap.skyrescue.domain.Drone;
import br.com.fiap.skyrescue.domain.DroneStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DroneRepository extends JpaRepository<Drone, Long> {

    Optional<Drone> findBySerialNumber(String serialNumber);

    List<Drone> findByStatus(DroneStatus status);

    boolean existsBySerialNumber(String serialNumber);
}
