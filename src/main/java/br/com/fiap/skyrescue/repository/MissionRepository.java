package br.com.fiap.skyrescue.repository;

import br.com.fiap.skyrescue.domain.Mission;
import br.com.fiap.skyrescue.domain.MissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    List<Mission> findByStatus(MissionStatus status);

    List<Mission> findByDroneId(Long droneId);
}
