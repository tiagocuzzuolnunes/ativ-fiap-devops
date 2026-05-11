package br.com.fiap.skyrescue.controller;

import br.com.fiap.skyrescue.dto.DroneRequest;
import br.com.fiap.skyrescue.dto.DroneResponse;
import br.com.fiap.skyrescue.service.DroneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/drones")
@RequiredArgsConstructor
@Tag(name = "Drones", description = "Gerenciamento de drones de resgate")
public class DroneController {

    private final DroneService droneService;

    @GetMapping
    @Operation(summary = "Lista todos os drones")
    public List<DroneResponse> list() {
        return droneService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca drone por ID")
    public DroneResponse findById(@PathVariable Long id) {
        return droneService.findById(id);
    }

    @PostMapping
    @Operation(summary = "Cadastra um novo drone")
    public ResponseEntity<DroneResponse> create(@Valid @RequestBody DroneRequest request) {
        DroneResponse created = droneService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/drones/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um drone existente")
    public DroneResponse update(@PathVariable Long id, @Valid @RequestBody DroneRequest request) {
        return droneService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um drone")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        droneService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
