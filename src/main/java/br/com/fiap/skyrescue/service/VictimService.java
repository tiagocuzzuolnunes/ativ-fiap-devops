package br.com.fiap.skyrescue.service;

import br.com.fiap.skyrescue.domain.Mission;
import br.com.fiap.skyrescue.domain.Victim;
import br.com.fiap.skyrescue.domain.VictimCondition;
import br.com.fiap.skyrescue.dto.VictimRequest;
import br.com.fiap.skyrescue.dto.VictimResponse;
import br.com.fiap.skyrescue.exception.ResourceNotFoundException;
import br.com.fiap.skyrescue.repository.VictimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VictimService {

    private final VictimRepository victimRepository;
    private final MissionService missionService;

    @Transactional(readOnly = true)
    public List<VictimResponse> findByMission(Long missionId) {
        return victimRepository.findByMissionId(missionId).stream().map(VictimResponse::from).toList();
    }

    public VictimResponse registerDetection(Long missionId, VictimRequest request) {
        Mission mission = missionService.loadMission(missionId);
        Victim victim = Victim.builder()
                .identification(request.identification())
                .condition(request.condition() != null ? request.condition() : VictimCondition.UNKNOWN)
                .latitude(request.latitude())
                .longitude(request.longitude())
                .detectionConfidence(request.detectionConfidence())
                .notes(request.notes())
                .mission(mission)
                .build();
        return VictimResponse.from(victimRepository.save(victim));
    }

    public void delete(Long id) {
        Victim victim = victimRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vitima nao encontrada: id=" + id));
        victimRepository.delete(victim);
    }
}
