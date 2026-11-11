package com.agnitio.application.ports.output;

import com.agnitio.domain.photographer.Photographer;
import com.agnitio.domain.photographer.PhotographerId;

import java.util.Optional;

public interface PhotographerRepository {
    Photographer save(Photographer photographer);
    Optional<Photographer> findById(PhotographerId id);
    Optional<Photographer> findByEmail(String email);
    boolean existsByEmail(String email);
}
