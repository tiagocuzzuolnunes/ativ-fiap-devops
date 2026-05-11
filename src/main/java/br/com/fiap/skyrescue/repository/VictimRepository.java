package br.com.fiap.skyrescue.repository;

import br.com.fiap.skyrescue.domain.Victim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VictimRepository extends JpaRepository<Victim, Long> {
    List<Victim> findByMissionId(Long missionId);
}
