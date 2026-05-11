package br.com.fiap.skyrescue.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "victims")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Victim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String identification;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VictimCondition condition = VictimCondition.UNKNOWN;

    @NotNull
    @Column(nullable = false)
    private Double latitude;

    @NotNull
    @Column(nullable = false)
    private Double longitude;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    @Column(nullable = false)
    @Builder.Default
    private Double detectionConfidence = 0.0;

    @Column(nullable = false)
    private LocalDateTime detectedAt;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @PrePersist
    void onCreate() {
        if (this.detectedAt == null) {
            this.detectedAt = LocalDateTime.now();
        }
    }
}
