package org.acme.repository;

import org.acme.model.PixComVencimento;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PixComVencimentoRepository implements PanacheRepository<PixComVencimento> {
    
}
