package com.example.medicalfield_local_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "medicamento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Medicamento {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    private String dosagem;
}