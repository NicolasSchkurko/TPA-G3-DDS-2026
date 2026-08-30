package ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.filaconverter;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Mail;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.MedioDeContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Telefono;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Mensaje.MedioDeContacto.Whatsapp;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.Juridica.Juridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.humana.Humana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donador.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.lector.csv.MapeoCSV;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PersonaDonanteFilaConverter implements FilaConverter<Donante> {

    private static final Logger logger = Logger.getLogger(PersonaDonanteFilaConverter.class.getName());

    public enum CampoLogico {
        TIPO_PERSONA, TIPO_DOC, DOCUMENTO, NOMBRE_RAZON_SOCIAL, EMAIL, TELEFONO, WHATSAPP
    }

    private final Map<CampoLogico, List<String>> mapeoColumnas;

    public PersonaDonanteFilaConverter(List<MapeoCSV> mapeosCsv) {
        if (mapeosCsv == null || mapeosCsv.isEmpty()) {
            throw new IllegalArgumentException("Se requiere una lista de MapeoCSV para saber cómo leer el archivo.");
        }

        this.mapeoColumnas = new HashMap<>();
        for (MapeoCSV mapeo : mapeosCsv) {
            try {
                CampoLogico campo = CampoLogico.valueOf(mapeo.getCampo().trim().toUpperCase());
                this.mapeoColumnas.put(campo, mapeo.getNombresColumnas());
            } catch (IllegalArgumentException e) {
                logger.warning("Campo mapeado no reconocido y será ignorado: " + mapeo.getCampo());
            }
        }
    }

    @Override
    public Donante convertir(Map<String, String> fila) {
        String tipoPersona = obtenerPrimerValor(fila, CampoLogico.TIPO_PERSONA)
                .orElse("")
                .trim()
                .toUpperCase();

        String documento = obtenerPrimerValor(fila, CampoLogico.DOCUMENTO).orElse("");
        String nombreRazonSocial = obtenerValorConcatenado(fila, CampoLogico.NOMBRE_RAZON_SOCIAL);

        Donante donante = instanciarDonante(tipoPersona, nombreRazonSocial, documento);
        vincularMediosDeContacto(donante, fila);

        return donante;
    }

    private Donante instanciarDonante(String tipoPersona, String nombreRazonSocial, String documento) {
        switch (tipoPersona) {
            case "HUMANA":
                return crearDonanteHumana(nombreRazonSocial, documento);
            case "JURIDICA":
                return crearDonanteJuridica(nombreRazonSocial, documento);
            default:
                throw new IllegalArgumentException("Tipo de persona no soportado: " + tipoPersona);
        }
    }

    private Donante crearDonanteHumana(String nombreCompleto, String documento) {
        String[] nombreYApellido = separarNombreYApellido(nombreCompleto);
        String nombre = nombreYApellido[0];
        String apellido = nombreYApellido[1];

        int numeroDocumento = limpiarYParsearDocumento(documento);

        Humana persona = new Humana(
                nombre,
                apellido,
                0,
                numeroDocumento,
                null);

        return new Donante(null, persona);
    }

    private Donante crearDonanteJuridica(String razonSocial, String documento) {
        Juridica persona = new Juridica(
                razonSocial,
                null,
                null,
                documento,
                new ArrayList<>());

        return new Donante(null, persona);
    }

    private void vincularMediosDeContacto(Donante donante, Map<String, String> fila) {
        if (donante == null || donante.getPersona() == null) {
            return;
        }

        Persona persona = donante.getPersona();

        String email = obtenerPrimerValor(fila, CampoLogico.EMAIL).orElse("");
        String telefono = obtenerPrimerValor(fila, CampoLogico.TELEFONO).orElse("");
        String whatsapp = obtenerPrimerValor(fila, CampoLogico.WHATSAPP).orElse("");

        if (!email.isBlank()) {
            persona.agregarMedioDeContacto(new Mail(email));
        }

        if (!telefono.isBlank()) {
            persona.agregarMedioDeContacto(new Telefono(telefono));
        }

        if (!whatsapp.isBlank()) {
            persona.agregarMedioDeContacto(new Whatsapp(whatsapp));
        }
    }

    private String[] separarNombreYApellido(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            return new String[]{"", ""};
        }

        if (!nombreCompleto.contains(" ")) {
            return new String[]{nombreCompleto.trim(), ""};
        }

        int ultimoEspacio = nombreCompleto.lastIndexOf(" ");
        String nombre = nombreCompleto.substring(0, ultimoEspacio).trim();
        String apellido = nombreCompleto.substring(ultimoEspacio + 1).trim();

        return new String[]{nombre, apellido};
    }

    private int limpiarYParsearDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            return 0;
        }

        try {
            String soloNumeros = documento.replaceAll("[^\\d]", "");
            if (soloNumeros.isBlank()) {
                return 0;
            }
            return Integer.parseInt(soloNumeros);
        } catch (NumberFormatException e) {
            logger.warning("No se pudo extraer un número válido del documento: " + documento);
            return 0;
        }
    }

    private String obtenerValorConcatenado(Map<String, String> fila, CampoLogico claveLogica) {
        List<String> columnas = mapeoColumnas.get(claveLogica);
        if (columnas == null) {
            return "";
        }

        Map<String, String> filaLimpia = limpiarBOM(fila);

        return columnas.stream()
                .map(filaLimpia::get)
                .filter(v -> v != null && !v.isBlank())
                .collect(Collectors.joining(" "))
                .trim();
    }

    private Optional<String> obtenerPrimerValor(Map<String, String> fila, CampoLogico claveLogica) {
        List<String> columnas = mapeoColumnas.get(claveLogica);
        if (columnas == null) {
            return Optional.empty();
        }

        Map<String, String> filaLimpia = limpiarBOM(fila);

        return columnas.stream()
                .map(filaLimpia::get)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    private Map<String, String> limpiarBOM(Map<String, String> fila) {
        Map<String, String> limpia = new HashMap<>();
        for (Map.Entry<String, String> entry : fila.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.startsWith("\uFEFF")) {
                key = key.substring(1);
            }
            limpia.put(key, entry.getValue());
        }
        return limpia;
    }
}