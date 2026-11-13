package ar.edu.utn.frba.ddsi.front.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Formulario de alta y edición de donantes del panel de administración (/admin/donantes).
 *
 * <p>Las secciones repetibles (medios de contacto y representantes) se bindean como <em>arrays
 * paralelos</em> de strings en vez de listas indexadas estilo {@code medios[0].tipo}. El motivo es
 * que las filas se agregan y se borran con JavaScript: con índices, borrar la fila del medio hace
 * que lleguen índices con huecos (0, 2, 3) y Spring falla o crea elementos vacíos. Con arrays
 * paralelos el navegador manda sólo los inputs que existen, en orden, y la correlación entre
 * {@code medioTipo[i]} y {@code medioValor[i]} se mantiene sola.
 *
 * <p>Todos los numéricos entran como String porque el admin puede dejarlos vacíos; la conversión
 * (y el default) se hace acá y no con el binder de Spring, que rechazaría "" para un int.
 */
@Getter
@Setter
public class FormularioDonanteDTO {

    /** Tipos que entiende MediosContactoDTO.toDomain() del backend; cualquier otro hace throw. */
    private static final Set<String> TIPOS_DE_CONTACTO = Set.of("EMAIL", "TELEFONO", "WHATSAPP");
    private static final Set<String> GENEROS = Set.of("HOMBRE", "MUJER", "OTRO");
    private static final Set<String> TIPOS_JURIDICOS = Set.of("ONG", "GUBERNAMENTAL", "EMPRESA", "INSTITUCION");

    private String tipoPersona;

    // Persona humana
    private String nombre;
    private String apellido;
    private String edad;
    private String numeroDeDocumento;
    private String genero;

    // Persona jurídica
    private String razonSocial;
    private String rubro;
    private String cuit;
    private String tipoJuridico;

    // Domicilio
    private String calleUno;
    private String calleDos;
    private String altura;
    private String piso;
    private String departamento;
    private String ciudad;
    private String provincia;
    private String pais;

    // Medios de contacto (filas repetibles)
    private String[] medioTipo;
    private String[] medioValor;
    /** Posición (0-based) del medio marcado como predeterminado. El JS renumera los radios. */
    private Integer medioPredeterminado;

    // Representantes (filas repetibles, sólo para persona jurídica)
    private String[] repNombre;
    private String[] repApellido;
    private String[] repEdad;
    private String[] repDocumento;
    private String[] repGenero;
    private String[] repActivo;

    public boolean esJuridica() {
        return "JURIDICA".equalsIgnoreCase(trim(tipoPersona));
    }

    /**
     * Arma el body que espera {@code PUT/POST /api/personas}.
     *
     * <p>Escribe <em>todas</em> las claves, incluso vacías o nulas, porque en la actualización el
     * payload se mergea con el estado actual del donante: si una lista no viajara, borrar el último
     * medio de contacto no tendría efecto y los datos viejos quedarían pegados.
     */
    public Map<String, Object> aPayload() {
        if (!esJuridica() && !"HUMANA".equalsIgnoreCase(trim(tipoPersona))) {
            throw new IllegalArgumentException("Seleccioná si el donante es persona humana o jurídica.");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tipoPersona", esJuridica() ? "JURIDICA" : "HUMANA");

        if (esJuridica()) {
            exigir(razonSocial, "la razón social");
            payload.put("razonSocial", trim(razonSocial));
            payload.put("rubro", trim(rubro));
            payload.put("cuit", trim(cuit));
            payload.put("tipoJuridico", opcionValida(tipoJuridico, TIPOS_JURIDICOS, "ONG"));
            // Los campos de persona humana se limpian para que no queden datos del tipo anterior.
            payload.put("nombre", null);
            payload.put("apellido", null);
            payload.put("edad", 0);
            // Juridica no tiene documento propio (el identificador es el cuit), pero el DTO del
            // backend declara numeroDeDocumento como int: va 0 para no dejar basura del tipo anterior.
            payload.put("numeroDeDocumento", 0);
            payload.put("genero", null);
            payload.put("representantes", representantes());
        } else {
            exigir(nombre, "el nombre");
            exigir(apellido, "el apellido");
            payload.put("nombre", trim(nombre));
            payload.put("apellido", trim(apellido));
            payload.put("edad", entero(edad));
            payload.put("numeroDeDocumento", entero(numeroDeDocumento));
            payload.put("genero", opcionValida(genero, GENEROS, "OTRO"));
            payload.put("razonSocial", null);
            payload.put("rubro", null);
            payload.put("cuit", null);
            payload.put("tipoJuridico", null);
            payload.put("representantes", List.of());
        }

        List<Map<String, String>> medios = medios();
        payload.put("mediosDeContacto", medios);
        payload.put("medioPredeterminado", predeterminado(medios));
        payload.put("direccion", direccion());
        return payload;
    }

    /** Filas con valor cargado, normalizadas. Las vacías se descartan en silencio. */
    private List<Map<String, String>> medios() {
        List<Map<String, String>> medios = new ArrayList<>();
        if (medioValor == null) {
            return medios;
        }

        for (int i = 0; i < medioValor.length; i++) {
            String valor = trim(medioValor[i]);
            if (valor == null) {
                continue;
            }
            String tipo = opcionValida(enPosicion(medioTipo, i), TIPOS_DE_CONTACTO, "EMAIL");
            if ("EMAIL".equals(tipo)) {
                // El backend construye Mail con valor.toLowerCase(); lo normalizamos acá también
                // para que la comparación del medio predeterminado no falle por mayúsculas.
                valor = valor.toLowerCase();
            }
            Map<String, String> medio = new LinkedHashMap<>();
            medio.put("tipo", tipo);
            medio.put("valor", valor);
            medios.add(medio);
        }
        return medios;
    }

    /**
     * El medio marcado con el radio, o el primero. Se clampea el índice porque las filas vacías se
     * descartan y el radio elegido puede haber quedado fuera de rango.
     */
    private Map<String, String> predeterminado(List<Map<String, String>> medios) {
        if (medios.isEmpty()) {
            return null;
        }
        int indice = medioPredeterminado != null ? medioPredeterminado : 0;
        return (indice >= 0 && indice < medios.size()) ? medios.get(indice) : medios.get(0);
    }

    private List<Map<String, Object>> representantes() {
        List<Map<String, Object>> representantes = new ArrayList<>();
        if (repNombre == null) {
            return representantes;
        }

        for (int i = 0; i < repNombre.length; i++) {
            String nombreRep = trim(repNombre[i]);
            String apellidoRep = trim(enPosicion(repApellido, i));
            if (nombreRep == null && apellidoRep == null) {
                continue;
            }
            Map<String, Object> representante = new LinkedHashMap<>();
            representante.put("nombre", nombreRep);
            representante.put("apellido", apellidoRep);
            representante.put("edad", entero(enPosicion(repEdad, i)));
            representante.put("numeroDeDocumento", entero(enPosicion(repDocumento, i)));
            // Ojo: RepresentanteDTO.toDomain() del backend hace Genero.valueOf(genero.toUpperCase())
            // sin chequear vacío, así que un "" rompería la request. Siempre va un valor válido.
            representante.put("genero", opcionValida(enPosicion(repGenero, i), GENEROS, "OTRO"));
            representante.put("activo", marcado(enPosicion(repActivo, i)));
            representantes.add(representante);
        }
        return representantes;
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

    /**
     * Filas de contacto tal como las tipeó el admin, para repintar el formulario cuando la carga
     * falla (así no pierde lo que escribió). Incluye las vacías: la idea es devolver la pantalla
     * igual a como la mandó.
     */
    public List<MediosContactoDTO> mediosPrecargados() {
        List<MediosContactoDTO> filas = new ArrayList<>();
        int cantidad = medioValor != null ? medioValor.length : 0;
        for (int i = 0; i < cantidad; i++) {
            MediosContactoDTO fila = new MediosContactoDTO();
            fila.setTipo(enPosicion(medioTipo, i));
            fila.setValor(medioValor[i]);
            filas.add(fila);
        }
        return filas;
    }

    /** Ídem {@link #mediosPrecargados()} para los representantes de una persona jurídica. */
    public List<RepresentanteDTO> representantesPrecargados() {
        List<RepresentanteDTO> filas = new ArrayList<>();
        int cantidad = repNombre != null ? repNombre.length : 0;
        for (int i = 0; i < cantidad; i++) {
            RepresentanteDTO fila = new RepresentanteDTO();
            fila.setNombre(repNombre[i]);
            fila.setApellido(enPosicion(repApellido, i));
            fila.setEdad(entero(enPosicion(repEdad, i)));
            fila.setNumeroDeDocumento(entero(enPosicion(repDocumento, i)));
            fila.setGenero(enPosicion(repGenero, i));
            fila.setActivo(marcado(enPosicion(repActivo, i)));
            filas.add(fila);
        }
        return filas;
    }

    /** Precarga el formulario con los datos actuales del donante (pantalla de edición). */
    public static FormularioDonanteDTO desde(PersonaDonanteDTO donante) {
        FormularioDonanteDTO formulario = new FormularioDonanteDTO();
        if (donante == null) {
            return formulario;
        }

        formulario.setTipoPersona(donante.getTipoPersona());
        formulario.setNombre(donante.getNombre());
        formulario.setApellido(donante.getApellido());
        formulario.setEdad(texto(donante.getEdad()));
        formulario.setNumeroDeDocumento(texto(donante.getNumeroDeDocumento()));
        formulario.setGenero(donante.getGenero());
        formulario.setRazonSocial(donante.getRazonSocial());
        formulario.setRubro(donante.getRubro());
        formulario.setCuit(donante.getCuit());
        formulario.setTipoJuridico(donante.getTipoJuridico());

        DireccionDTO direccion = donante.getDireccion();
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

        formulario.setMedioPredeterminado(indiceDelPredeterminado(donante));
        return formulario;
    }

    /**
     * Posición del medio predeterminado dentro de la lista de medios, para marcar el radio correcto
     * en el formulario de edición. El backend no devuelve el índice sino el medio repetido aparte,
     * así que se busca por tipo + valor igual que hace {@code resolverMedioPredeterminado()} allá.
     */
    private static Integer indiceDelPredeterminado(PersonaDonanteDTO donante) {
        List<MediosContactoDTO> medios = donante.getMediosDeContacto();
        MediosContactoDTO predeterminado = donante.getMedioPredeterminado();
        if (medios == null || medios.isEmpty() || predeterminado == null) {
            return null;
        }

        for (int i = 0; i < medios.size(); i++) {
            MediosContactoDTO medio = medios.get(i);
            if (medio == null) {
                continue;
            }
            boolean mismoTipo = opcionValida(medio.getTipo(), TIPOS_DE_CONTACTO, "EMAIL")
                    .equals(opcionValida(predeterminado.getTipo(), TIPOS_DE_CONTACTO, "EMAIL"));
            boolean mismoValor = medio.getValor() != null
                    && medio.getValor().equalsIgnoreCase(predeterminado.getValor());
            if (mismoTipo && mismoValor) {
                return i;
            }
        }
        return null;
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
            throw new IllegalArgumentException("Completá " + queFalta + " del donante.");
        }
    }

    private static String opcionValida(String valor, Set<String> permitidos, String porDefecto) {
        String normalizado = trim(valor);
        if (normalizado == null) {
            return porDefecto;
        }
        normalizado = normalizado.toUpperCase();
        // MAIL es el alias histórico que todavía devuelve el backend para los mails.
        if ("MAIL".equals(normalizado)) {
            normalizado = "EMAIL";
        }
        return permitidos.contains(normalizado) ? normalizado : porDefecto;
    }

    /**
     * "activo" del representante. Viaja como &lt;select&gt; y no como checkbox a propósito: un
     * checkbox sin marcar no se manda, y eso correría una posición el array repActivo respecto de
     * repNombre, mezclando los datos de filas distintas. Sin valor, el representante queda activo.
     */
    private static boolean marcado(String valor) {
        String normalizado = trim(valor);
        return normalizado == null || !"false".equalsIgnoreCase(normalizado);
    }

    private static int entero(String valor) {
        String normalizado = trim(valor);
        if (normalizado == null) {
            return 0;
        }
        // Se descartan separadores y guiones (CUIT 30-12345678-9, documentos con puntos).
        String soloDigitos = normalizado.replaceAll("\\D", "");
        if (soloDigitos.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(soloDigitos);
        } catch (NumberFormatException e) {
            // Un CUIT completo no entra en un int: el backend lo guarda igual en el campo cuit.
            return 0;
        }
    }

    private static String enPosicion(String[] valores, int indice) {
        return valores != null && indice < valores.length ? valores[indice] : null;
    }

    private static String trim(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }
}
