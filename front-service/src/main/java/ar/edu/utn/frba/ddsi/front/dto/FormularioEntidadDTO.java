package ar.edu.utn.frba.ddsi.front.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Formulario de alta y edición de entidades beneficiarias del panel de administración
 * (/admin/entidades/nuevo y /admin/entidades/{id}/editar).
 *
 * <p>Es más chico que {@link FormularioDonanteDTO} porque una entidad es siempre una persona
 * jurídica: no hay que elegir tipo de persona ni hay secciones repetibles. Los medios de contacto
 * de la entidad son exactamente dos y fijos (un teléfono y un mail), tal como los modela
 * {@code EntidadBeneficiariaDTO.toDomain()} del backend, así que viajan como campos planos.
 *
 * <p>Los numéricos del domicilio entran como String porque el admin puede dejarlos vacíos; la
 * conversión se hace acá y no con el binder de Spring, que rechazaría "" para un Integer.
 */
@Getter
@Setter
public class FormularioEntidadDTO {

    /** Valores que acepta TipoJuridico en el backend; cualquier otro cae en el default. */
    private static final Set<String> TIPOS_JURIDICOS = Set.of("ONG", "GUBERNAMENTAL", "EMPRESA", "INSTITUCION");

    // Datos de la organización
    private String razonSocial;
    private String rubro;
    private String cuit;
    private String tipoJuridico;

    // Contacto
    private String telefono;
    private String email;

    // Domicilio (punto de entrega que usa logística)
    private String calleUno;
    private String calleDos;
    private String altura;
    private String piso;
    private String departamento;
    private String ciudad;
    private String provincia;
    private String pais;

    /**
     * Arma el body que espera {@code POST/PUT /api/entidades}.
     *
     * <p>Escribe todas las claves aunque estén vacías: el PUT del backend reconstruye la jurídica
     * desde cero con lo que venga, así que un campo ausente y un campo nulo tienen el mismo efecto
     * y mandar todo deja explícito que vaciar un dato lo borra.
     */
    public Map<String, Object> aPayload() {
        exigir(razonSocial, "la razón social");

        String correo = trim(email);
        if (correo != null) {
            correo = correo.toLowerCase();
            // El backend hace throw si el mail no tiene arroba; el mensaje propio es más claro.
            if (!correo.contains("@")) {
                throw new IllegalArgumentException(
                        "El correo electrónico no parece válido: le falta el @.");
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("razonSocial", trim(razonSocial));
        payload.put("rubro", trim(rubro));
        payload.put("cuit", trim(cuit));
        payload.put("tipoJuridico", opcionValida(tipoJuridico, TIPOS_JURIDICOS, "ONG"));
        payload.put("telefono", trim(telefono));
        // email y correoElectronico apuntan al mismo Mail en el backend (uno es alias del otro):
        // se mandan los dos para que da igual cuál lea.
        payload.put("email", correo);
        payload.put("correoElectronico", correo);
        payload.put("direccion", direccion());
        return payload;
    }

    /** null si no se cargó ningún campo: así el backend no crea una dirección fantasma. */
    private Map<String, Object> direccion() {
        Map<String, Object> direccion = new LinkedHashMap<>();
        ponerSiTiene(direccion, "calleUno", calleUno);
        ponerSiTiene(direccion, "calleDos", calleDos);
        ponerSiTiene(direccion, "departamento", departamento);
        ponerSiTiene(direccion, "ciudad", ciudad);
        ponerSiTiene(direccion, "provincia", provincia);
        ponerSiTiene(direccion, "pais", pais);
        if (entero(altura) != 0) {
            direccion.put("altura", entero(altura));
        }
        if (entero(piso) != 0) {
            direccion.put("piso", entero(piso));
        }
        return direccion.isEmpty() ? null : direccion;
    }

    /** Precarga el formulario con los datos actuales de la entidad (pantalla de edición). */
    public static FormularioEntidadDTO desde(EntidadBeneficiariaDTO entidad) {
        FormularioEntidadDTO formulario = new FormularioEntidadDTO();
        if (entidad == null) {
            return formulario;
        }

        formulario.setRazonSocial(entidad.getRazonSocial());
        formulario.setRubro(entidad.getRubro());
        formulario.setCuit(entidad.getCuit());
        formulario.setTipoJuridico(entidad.getTipoJuridico());
        formulario.setTelefono(entidad.getTelefono());
        // Los dos campos son el mismo Mail, pero una entidad vieja puede tener sólo el alias.
        formulario.setEmail(entidad.getEmail() != null && !entidad.getEmail().isBlank()
                ? entidad.getEmail()
                : entidad.getCorreoElectronico());

        DireccionDTO direccion = entidad.getDireccion();
        if (direccion != null) {
            formulario.setCalleUno(direccion.getCalleUno());
            formulario.setCalleDos(direccion.getCalleDos());
            formulario.setAltura(texto(direccion.getAltura()));
            formulario.setPiso(texto(direccion.getPiso()));
            formulario.setDepartamento(direccion.getDepartamento());
            formulario.setCiudad(direccion.getCiudad());
            formulario.setProvincia(direccion.getProvincia());
            formulario.setPais(direccion.getPais());
        }

        return formulario;
    }

    private static String texto(Integer valor) {
        return valor == null || valor == 0 ? "" : String.valueOf(valor);
    }

    private static void ponerSiTiene(Map<String, Object> destino, String clave, String valor) {
        if (trim(valor) != null) {
            destino.put(clave, trim(valor));
        }
    }

    private static void exigir(String valor, String queFalta) {
        if (trim(valor) == null) {
            throw new IllegalArgumentException("Completá " + queFalta + " de la entidad.");
        }
    }

    private static String opcionValida(String valor, Set<String> permitidos, String porDefecto) {
        String normalizado = trim(valor);
        if (normalizado == null) {
            return porDefecto;
        }
        normalizado = normalizado.toUpperCase();
        return permitidos.contains(normalizado) ? normalizado : porDefecto;
    }

    private static int entero(String valor) {
        String normalizado = trim(valor);
        if (normalizado == null) {
            return 0;
        }
        // Se descartan separadores y guiones (alturas tipeadas como "1.234").
        String soloDigitos = normalizado.replaceAll("\\D", "");
        if (soloDigitos.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(soloDigitos);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String trim(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }
}
