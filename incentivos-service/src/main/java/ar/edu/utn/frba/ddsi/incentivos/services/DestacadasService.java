package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.dto.Perfil.DonacionDestacadaDTO;
import ar.edu.utn.frba.ddsi.incentivos.models.repositories.SpringRepositories.RepositorioDonaciones;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Donaciones entregadas que completaron una misión, para el landing público. */
@Service
public class DestacadasService {

    /** Tope del parámetro {@code limite} para no cargar la tabla completa. */
    public static final int LIMITE_MAXIMO = 100;

    private final RepositorioDonaciones repositorio;
    private final int limitePredeterminado;

    public DestacadasService(
            RepositorioDonaciones repositorio,
            @Value("${incentivos.destacadas.limite:30}") int limitePredeterminado) {
        this.repositorio = repositorio;
        this.limitePredeterminado = limitePredeterminado;
    }

    /**
     * Devuelve las últimas donaciones destacadas. Sin límite explícito usa la property.
     *
     * @param limite cantidad solicitada; {@code null} usa el valor configurado
     * @return lista vacía si no hay donaciones destacadas
     */
    @Transactional(readOnly = true)
    public List<DonacionDestacadaDTO> obtenerDestacadas(Integer limite) {
        int efectivo = limite == null ? limitePredeterminado : limite;

        if (efectivo < 1 || efectivo > LIMITE_MAXIMO) {
            throw new IllegalArgumentException(
                    "El límite debe estar entre 1 y " + LIMITE_MAXIMO);
        }

        return repositorio.buscarDestacadas(PageRequest.of(0, efectivo)).stream()
                .map(donacion -> new DonacionDestacadaDTO(
                        donacion.getIdDonacion(),
                        donacion.getFechaEntrega(),
                        donacion.getCantidadBienes(),
                        donacion.getCategoria(),
                        donacion.getSubCategoria(),
                        donacion.getEntidadBeneficiaria()))
                .toList();
    }
}
