package ar.edu.utn.frba.ddsi.front.services;

import ar.edu.utn.frba.ddsi.front.dto.DonacionDTO;
import ar.edu.utn.frba.ddsi.front.dto.DireccionDTO;
import ar.edu.utn.frba.ddsi.front.dto.EntidadBeneficiariaDTO;
import ar.edu.utn.frba.ddsi.front.dto.NecesidadDTO;
import ar.edu.utn.frba.ddsi.front.dto.SeguimientoDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Datos de demostración para cuando el usuario todavía no vinculó su entidad o el backend no responde.
 * Siempre se muestran con el banner "Estás viendo datos de demostración".
 */
public final class DemoDataService {

    public static final UUID ID_DEMO = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private DemoDataService() {
    }

    public static EntidadBeneficiariaDTO entidadDemo() {
        EntidadBeneficiariaDTO entidad = new EntidadBeneficiariaDTO();
        entidad.setId(ID_DEMO);
        entidad.setRazonSocial("Comedor Los Piletones");
        entidad.setRubro("Asistencia social");
        entidad.setCuit("30-12345678-9");
        entidad.setTipoJuridico("ONG");
        entidad.setTelefono("+54 11 4567-8900");
        entidad.setActiva(true);

        DireccionDTO direccion = new DireccionDTO();
        direccion.setCalleUno("Av. Perito Moreno");
        direccion.setAltura(2400);
        direccion.setCiudad("Villa Soldati");
        direccion.setProvincia("Buenos Aires");
        direccion.setPais("Argentina");
        entidad.setDireccion(direccion);

        entidad.setNecesidades(List.of(
                necesidad("Alimentos no perecederos", "Alimentos", "Arroz, fideos y leche en polvo",
                        240, 165, "RECURRENTE", 45, true),
                necesidad("Ropa de abrigo", "Textil", "Camperas y frazadas para el invierno",
                        120, 40, "EXTRAORDINARIA", null, true),
                necesidad("Medicamentos básicos", "Salud", "Antitérmicos y analgésicos",
                        500, 500, "RECURRENTE", 30, false)
        ));
        return entidad;
    }

    public static List<DonacionDTO> donacionesDemo() {
        return List.of(
                donacion("Arroz y fideos x 1.200 kg", "ASIGNADO", "Alimentos no perecederos", "Comercial del Sur", 1600, 7),
                donacion("Frazadas x 80 unidades", "EN_TRASLADO", "Ropa de abrigo", "Textil Norte", 80, 2),
                donacion("Paracetamol x 300 blíster", "ENTREGADO", "Medicamentos", "Droguería Central", 300, 30)
        );
    }

    /** Seguimiento simulado coherente con las donaciones demo (los camiones se posicionan en JS/Java demo). */
    public static SeguimientoDTO seguimientoDemo() {
        SeguimientoDTO dto = new SeguimientoDTO();
        dto.setUbicacionSimulada(true);
        dto.setGeneradoEn(java.time.OffsetDateTime.now().toString());

        SeguimientoDTO.EntregaActiva arroz = new SeguimientoDTO.EntregaActiva();
        arroz.setIdDonacion(UUID.fromString("00000000-0000-4000-8000-0000000000a1"));
        arroz.setEstado("EN_CAMINO");
        arroz.setCantidad(1600);
        arroz.setUnidadDeMedida("KG");
        arroz.setDestino("Av. Perito Moreno 2400");
        arroz.setZona("Villa Soldati");
        arroz.setLat(-34.6538);
        arroz.setLng(-58.4478);

        SeguimientoDTO.EntregaActiva frazadas = new SeguimientoDTO.EntregaActiva();
        frazadas.setIdDonacion(UUID.fromString("00000000-0000-4000-8000-0000000000a2"));
        frazadas.setEstado("PENDIENTE");
        frazadas.setCantidad(80);
        frazadas.setUnidadDeMedida("UNIDADES");
        frazadas.setDestino("Av. Perito Moreno 2400");
        frazadas.setZona("Villa Soldati");
        frazadas.setLat(-34.6500);
        frazadas.setLng(-58.4600);

        dto.setEntregas(List.of(arroz, frazadas));

        SeguimientoDTO.CamionEnRuta camion = new SeguimientoDTO.CamionEnRuta();
        camion.setPatente("AB123CD");
        camion.setChofer("Chofer de demo");
        camion.setEstadoRuta("EN_CURSO");
        camion.setLat(-34.6405);
        camion.setLng(-58.4550);
        camion.setUltimaActualizacion(java.time.OffsetDateTime.now().toString());
        camion.setEtaMinutos(14);
        camion.setDonaciones(List.of(arroz.getIdDonacion(), frazadas.getIdDonacion()));
        dto.setCamiones(List.of(camion));
        return dto;
    }

    private static NecesidadDTO necesidad(String subcategoria, String categoria, String descripcion,
                                          int objetivo, int recibidos, String tipo, Integer plazo, boolean activa) {
        NecesidadDTO n = new NecesidadDTO();
        n.setDescripcion(descripcion);
        n.setCantidadObjetivo(objetivo);
        n.setNombreSubcategoria(subcategoria);
        n.setNombreCategoria(categoria);
        n.setTipoNecesidad(tipo);
        n.setPlazoEnDias(plazo);
        n.setActiva(activa);
        n.setId(UUID.randomUUID());
        DonacionDTO d = new DonacionDTO();
        d.setId(UUID.randomUUID());
        d.setDescripcion("Aporte demo");
        d.setEstado("ASIGNADO");
        d.setCantidadTotalBienes(recibidos);
        d.setFechaEntrega(LocalDate.now());
        n.setDonaciones(List.of(d));
        return n;
    }

    private static DonacionDTO donacion(String descripcion, String estado, String categoria, String donante,
                                        int cantidad, int dias) {
        DonacionDTO d = new DonacionDTO();
        d.setId(UUID.randomUUID());
        d.setDescripcion(descripcion);
        d.setEstado(estado);
        d.setCategoriaBienName(categoria);
        d.setDonanteName(donante);
        d.setCantidadTotalBienes(cantidad);
        d.setFechaEntrega(LocalDate.now().plusDays(dias));
        return d;
    }
}
