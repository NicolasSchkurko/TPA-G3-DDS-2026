package ar.edu.utn.frba.ddsi.donaciones.models.repositories.interfaces;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para Persona (incluye Humana y Juridica, mapeadas con herencia JOINED).
 * RepositorioPersonas actúa como fachada sobre esta interfaz (ver EntidadBeneficiariaJpaRepository).
 */
public interface PersonaJpaRepository extends JpaRepository<Persona, UUID> {
}
