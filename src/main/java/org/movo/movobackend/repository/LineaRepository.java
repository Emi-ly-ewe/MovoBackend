package org.movo.movobackend.repository;

import org.movo.movobackend.model.entity.LineaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LineaRepository extends JpaRepository<LineaEntity, String> {
    List<LineaEntity> findByOperador(String operador);
}