package ar.edu.utn.frba.ddsi.front.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Un bien dentro del formulario de donación. Espeja al BienResumenDTO de donaciones-service, así
 * que sirve tanto para mandar (POST /api/donaciones/formulario, PUT /api/donaciones/{id}) como
 * para leer los bienes que ya tiene una donación.
 *
 * <p>categoria y subcategoria no son obligatorias para el backend (si faltan usa "General"), pero
 * la subcategoría es la que decide todo lo que viene después: el segmentador agrupa por ella y el
 * matchmaking sólo propone necesidades de esa misma subcategoría. El formulario del donante no las
 * pide; el del admin sí, justamente para que la donación pueda matchear con alguna necesidad.
 */
@Getter
@Setter
public class BienDonacionDTO {

    @NotBlank(message = "Completá la descripción de cada bien.")
    private String descripcion;

    @NotBlank(message = "Seleccioná una categoría para cada bien.")
    private String categoria;

    @NotBlank(message = "Seleccioná una subcategoría para cada bien.")
    private String subcategoria;

    private String urlFoto;

    @NotNull(message = "Indicá la cantidad de cada bien.")
    @Positive(message = "La cantidad debe ser mayor a cero.")
    private Integer cantidad;

    @NotBlank(message = "Seleccioná la unidad de medida.")
    private String unidadDeMedida;

    @NotBlank(message = "Seleccioná el tipo de bien.")
    private String tipoBien;

    private Boolean usado;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaVencimiento;

    /** Fila que el admin abrió y no usó: se descarta sin avisar. */
    public boolean estaVacio() {
        return vacio(descripcion) && cantidad == null && vacio(categoria) && vacio(subcategoria);
    }

    public boolean esPerecedero() {
        return "PERECEDERO".equalsIgnoreCase(tipoBien);
    }

    /** Texto de una línea para listar el bien: "Arroz — 20 KILOGRAMOS (Alimentos / Arroz)". */
    public String resumen() {
        StringBuilder texto = new StringBuilder(vacio(descripcion) ? "Bien sin descripción" : descripcion.trim());
        if (cantidad != null) {
            texto.append(" — ").append(cantidad);
            if (!vacio(unidadDeMedida)) {
                texto.append(' ').append(unidadDeMedida.toLowerCase());
            }
        }
        if (!vacio(subcategoria)) {
            texto.append(" (").append(vacio(categoria) ? subcategoria.trim() : categoria.trim() + " / " + subcategoria.trim()).append(')');
        }
        return texto.toString();
    }

    /**
     * Deja la lista lista para mandar al backend: descarta filas vacías, recorta, completa los
     * valores que el backend exige y limpia los campos que no corresponden al tipo de bien.
     *
     * <p>Lo último importa porque BienResumenDTO.toDomain() elige la subclase por tipoBien y lo
     * demás lo ignora: un PERECEDERO con "usado" marcado guardaría un BienPerecedero y el flag se
     * perdería en silencio. Mejor no mandarlo.
     */
    public static List<BienDonacionDTO> normalizar(List<BienDonacionDTO> bienes) {
        List<BienDonacionDTO> limpios = new ArrayList<>();
        if (bienes == null) {
            return limpios;
        }

        for (BienDonacionDTO bien : bienes) {
            if (bien == null || bien.estaVacio()) {
                continue;
            }
            limpios.add(bien.normalizado(limpios.size() + 1));
        }
        return limpios;
    }

    /** @param numeroDeFila posición 1-based, sólo para que el mensaje de error diga cuál falló. */
    private BienDonacionDTO normalizado(int numeroDeFila) {
        descripcion = recortar(descripcion);
        categoria = recortar(categoria);
        subcategoria = recortar(subcategoria);
        urlFoto = recortar(urlFoto);
        unidadDeMedida = mayusculas(unidadDeMedida);
        tipoBien = mayusculas(tipoBien);

        if (cantidad == null || cantidad <= 0) {
            throw new IllegalArgumentException(
                    "El bien " + numeroDeFila + " necesita una cantidad mayor a cero.");
        }
        if (vacio(tipoBien)) {
            throw new IllegalArgumentException(
                    "Elegí el tipo del bien " + numeroDeFila + " (con estado o perecedero).");
        }
        if (!"CON_ESTADO".equals(tipoBien) && !"PERECEDERO".equals(tipoBien)) {
            throw new IllegalArgumentException("Tipo de bien desconocido: " + tipoBien);
        }
        // El backend sólo entiende KILOGRAMOS y LITROS; cualquier otra cosa la mapea a null y la
        // clave de segmentación queda sin unidad, así que conviene cortarlo acá.
        if (vacio(unidadDeMedida)) {
            throw new IllegalArgumentException(
                    "Elegí la unidad de medida del bien " + numeroDeFila + ".");
        }
        if (!"UNIDADES".equals(unidadDeMedida) && !"KILOGRAMOS".equals(unidadDeMedida)
                && !"LITROS".equals(unidadDeMedida)) {
            throw new IllegalArgumentException("Unidad de medida desconocida: " + unidadDeMedida);
        }

        if (esPerecedero()) {
            usado = null;
            if (fechaVencimiento == null) {
                throw new IllegalArgumentException(
                        "El bien " + numeroDeFila + " es perecedero: indicá su fecha de vencimiento.");
            }
        } else {
            fechaVencimiento = null;
            if (usado == null) {
                usado = false;
            }
        }

        // "General" es el default del backend; se escribe explícito para que el admin vea en el
        // listado la misma subcategoría con la que el matchmaking va a trabajar.
        if (vacio(categoria)) {
            categoria = "General";
        }
        if (vacio(subcategoria)) {
            subcategoria = "General";
        }
        if (vacio(descripcion)) {
            descripcion = subcategoria;
        }
        return this;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static String recortar(String valor) {
        return vacio(valor) ? null : valor.trim();
    }

    private static String mayusculas(String valor) {
        return vacio(valor) ? null : valor.trim().toUpperCase();
    }
}
