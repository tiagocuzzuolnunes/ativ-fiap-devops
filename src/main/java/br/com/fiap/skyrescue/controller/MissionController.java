package br.com.fiap.skyrescue.controller;

import br.com.fiap.skyrescue.domain.MissionStatus;
import br.com.fiap.skyrescue.dto.MissionRequest;
import br.com.fiap.skyrescue.dto.MissionResponse;
import br.com.fiap.skyrescue.dto.VictimRequest;
import br.com.fiap.skyrescue.dto.VictimResponse;
import br.com.fiap.skyrescue.service.MissionService;
import br.com.fiap.skyrescue.service.VictimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/missions")
@RequiredArgsConstructor
@Tag(name = "Missoes", description = "Orquestracao de missoes de resgate")
public class MissionController {

    private final MissionService missionService;
    private final VictimService victimService;

    @GetMapping
    @Operation(summary = "Lista todas as missoes")
    public List<MissionResponse> list() {
        return missionService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca missao por ID")
    public MissionResponse findById(@PathVariable Long id) {
        return missionService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Cria uma nova missao de resgate")
    public ResponseEntity<MissionResponse> create(@Valid @RequestBody MissionRequest request) {
        MissionResponse created = missionService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/missions/" + created.id())).body(created);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualiza o status de uma missao")
    public MissionResponse updateStatus(@PathVariable Long id, @RequestParam MissionStatus status) {
        return missionService.updateStatus(id, status);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma missao")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        missionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/victims")
    @Operation(summary = "Lista as vitimas detectadas em uma missao")
    public List<VictimResponse> victims(@PathVariable Long id) {
        return victimService.findByMission(id);
    }

    @PostMapping("/{id}/victims")
    @Operation(summary = "Registra a deteccao de uma vitima durante a missao")
    public ResponseEntity<VictimResponse> registerVictim(@PathVariable Long id,
                                                         @Valid @RequestBody VictimRequest request) {
        VictimResponse created = victimService.registerDetection(id, request);
        return ResponseEntity.created(URI.create("/api/v1/missions/" + id + "/victims/" + created.id())).body(created);
    }
}
