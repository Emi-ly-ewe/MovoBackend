package org.movo.movobackend.repository;

import org.movo.movobackend.model.entity.ParadaEntity;
import org.movo.movobackend.model.entity.ParadaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParadaRepository extends JpaRepository<ParadaEntity, ParadaId> {
    List<ParadaEntity> findByLineaAndSentidoOrderByOrdenParadaAsc(String linea, Integer sentido);
}