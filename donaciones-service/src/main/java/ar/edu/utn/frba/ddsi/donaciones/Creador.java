package ar.edu.utn.frba.ddsi.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Bienes.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.AsignadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Donaciones.SegmentadorDonaciones;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.EntidadBeneficiaria.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.MedioDeContacto.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Necesidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.*;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.direccion.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public interface Creador {
    Scanner scanner = new Scanner(System.in);
    static Humano crearHumano(){
        System.out.println("Elija el nombre de la persona");
        String nombre = scanner.nextLine().trim();
        System.out.println("Elija el apellido de la persona");
        String apellido = scanner.nextLine().trim();
        System.out.println("Elija la edad de la persona");
        int edad = scanner.nextInt();
        System.out.println("Elija el numero de documento de la persona");
        int numeroDeDocumento = scanner.nextInt();
        scanner.nextLine();
        System.out.println("Elija el genero de la persona");
        Genero genero = Genero.valueOf(scanner.nextLine().trim().toUpperCase());
        return new Humano(nombre, apellido, edad, numeroDeDocumento, genero);
    }

    static Direccion crearDireccion(){
        System.out.println("Elija el pais de la persona");
        String pais = scanner.nextLine().trim();
        System.out.println("Elija la provincia de la persona");
        String provincia = scanner.nextLine().trim();
        System.out.println("Elija la ciudad de la persona");
        String ciudadNombre = scanner.nextLine().trim();
        Ciudad ciudad = new Ciudad(ciudadNombre, new Provincia(provincia, new Pais(pais)));
        System.out.println("Elija el departamento de la persona");
        String departamento = scanner.nextLine().trim();
        System.out.println("Elija el piso de la persona");
        int piso = scanner.nextInt();
        System.out.println("Elija la altura de donde vive la persona");
        Integer altura = scanner.nextInt();
        scanner.nextLine();
        System.out.println("Elija la primera calle de la persona");
        String calleUno = scanner.nextLine().trim();
        System.out.println("Elija la segunda calle de la persona");
        String calleDos = scanner.nextLine().trim();
        return new Direccion(calleUno, calleDos, altura, piso, departamento, ciudad);
    }

    static void crearPersonaHumana(){
        Humano humano = Creador.crearHumano();
        System.out.println(humano);
        Direccion direccion = Creador.crearDireccion();
        System.out.println(direccion);
        PersonaHumana persona = new PersonaHumana(humano, direccion);
        System.out.println(persona);
        GestorPersonas.getInstance().agregarPersona(persona);
    }

    static void crearPersonaJuridica(){
        String seleccion;
        System.out.println("Elija el nombre de la organizacion");
        String nombre = scanner.nextLine().trim();
        Direccion direccion = Creador.crearDireccion();
        System.out.println(direccion);
        System.out.println("Elija la razon social de la organizacion");
        String razonSocial = scanner.nextLine().trim();
        System.out.println("Elija el rubro de la organizacion");
        String rubro = scanner.nextLine().trim();
        System.out.println("Elija el tipo juridico de la organizacion");
        TipoJuridico tipoJuridico = TipoJuridico.valueOf(scanner.nextLine().trim().toUpperCase());
        System.out.println("Elija el cuit de la organizacion");
        String cuit = scanner.nextLine().trim();
        System.out.println("Registre los representantes de la organizacion");
        List<Representante> representantes = new ArrayList<>();
        while(true){
            Humano humano = Creador.crearHumano();
            System.out.println("¿Esta activo este representante? s/n ");
            boolean activo;
            while(true){
                seleccion = scanner.nextLine().trim();
                if(seleccion.equalsIgnoreCase("s") || seleccion.equalsIgnoreCase("n")){
                    break;
                }
                System.out.println("Elija una opcion valida");
            }
            activo = seleccion.equalsIgnoreCase("s");
            Representante representate = new Representante(humano, activo);
            System.out.println(representate);
            representantes.add(representate);
            System.out.println("¿Desea agregar otro representante? s/n");
            while(true){
                seleccion = scanner.nextLine().trim();
                if(seleccion.equalsIgnoreCase("s") || seleccion.equalsIgnoreCase("n")){
                    break;
                }
                System.out.println("Elija una opcion valida");
            }
            if (seleccion.equalsIgnoreCase("n")){
                break;
            }
        }
        // Usar el constructor correcto y luego setear el nombre (la clase PersonaJuridica no recibe 'nombre' en su constructor)
        PersonaJuridica persona = new PersonaJuridica(direccion, razonSocial, rubro, tipoJuridico, cuit, representantes);
        persona.setNombre(nombre);
        System.out.println(persona);
        GestorPersonas.getInstance().agregarPersona(persona);
    }

    static void crearEntidad(){
        String seleccion;
        System.out.println("Vamos a registrar una entidad beneficiaria");
        System.out.println("Elija la razon social de la entidad");
        String razonSocial = scanner.nextLine().trim();
        Direccion direccion = Creador.crearDireccion();
        System.out.println("Elija el numero de telefono de la entidad");
        String nroTell = scanner.nextLine().trim();
        Telefono telefono = new Telefono(nroTell);
        List<MedioDeContacto> listaMediosDeContacto = new ArrayList<>();
        while(true){
            System.out.println("Escriba el mail de contacto del representante de la entidad");
            listaMediosDeContacto.add(new Mail(scanner.nextLine().trim()));
            System.out.println("¿Desea agregar otro mail de contacto? s/n");
            while(true){
                seleccion = scanner.nextLine().trim();
                if(seleccion.equalsIgnoreCase("s") || seleccion.equalsIgnoreCase("n")){
                    break;
                }
                System.out.println("Elija una opcion valida");
            }
            if (seleccion.equalsIgnoreCase("n")){
                break;
            }
        }
        MediosDeContacto mediosDeContacto = new MediosDeContacto();
        mediosDeContacto.agregarMediosDeContacto(listaMediosDeContacto);
        EntidadBeneficiaria entidad = new EntidadBeneficiaria(razonSocial, direccion, telefono, mediosDeContacto);
        System.out.println("Ahora registremos las necesidades de la entidad");
        Creador.crearNecesidades(entidad);
        System.out.println(entidad);
        AsignadorDonaciones.getInstance().agregarEntidad(entidad);
    }

    static void crearNecesidades(EntidadBeneficiaria entidad){
        String seleccion;
        while(true){
            String necesidad;
            while(true){
                System.out.println("¿Es la necesidad extraordinaria o recurrente? e/r");
                seleccion = scanner.nextLine().trim();
                if(seleccion.equalsIgnoreCase("e") || seleccion.equalsIgnoreCase("r")){
                    necesidad = seleccion;
                    break;
                }
                System.out.println("Elija una opcion valida");
            }
            System.out.println("Elija a que categoria pertenece la necesidad");
            CategoriaBien categoria = CategoriaBien.valueOf(scanner.nextLine().trim().toUpperCase());
            System.out.println("Elija el nombre de la necesidad");
            String nombre = scanner.nextLine().trim();
            System.out.println("Escriba una descripcion de la necesidad");
            String descripcion = scanner.nextLine().trim();
            System.out.println("Elija la cantidad objetivo de la necesidad");
            Integer cantidadObjetivo = scanner.nextInt();
            if (necesidad.equalsIgnoreCase("e")){
                NecesidadExtraordinaria necesidades = new NecesidadExtraordinaria(new SubcategoriaBien(nombre, categoria), descripcion, cantidadObjetivo);
                System.out.println(necesidades);
                entidad.agregarNecesidad(necesidades);
            } else {
                System.out.println("Elija el plazo en dias que necesita la necesidad");
                Integer plazoEnDias = scanner.nextInt();
                NecesidadRecurrente necesidades = new NecesidadRecurrente(new SubcategoriaBien(nombre, categoria), descripcion, cantidadObjetivo, plazoEnDias);
                System.out.println(necesidades);
                entidad.agregarNecesidad(necesidades);
             }
            System.out.println("¿Desea agregar otra necesidad? s/n");
            scanner.nextLine();
            while(true){
                seleccion = scanner.nextLine().trim();
                if(seleccion.equalsIgnoreCase("s") || seleccion.equalsIgnoreCase("n")){
                    break;
                }
                System.out.println("Elija una opcion valida");
            }
            if (seleccion.equalsIgnoreCase("n")){
                break;
            }
        }
    }

    static void crearDonaciones() {
        String seleccion;
        PersonaDonante persona;
        System.out.println("Vamos a realizar una donacion");
        while(true){
            System.out.println("¿Esta usted registrado como donante? s/n");
            while(true){
                seleccion = scanner.nextLine().trim();
                if(seleccion.equalsIgnoreCase("s") || seleccion.equalsIgnoreCase("n")){
                    break;
                }
                System.out.println("Elija una opcion valida");
            }
            if (seleccion.equalsIgnoreCase("n")){
                System.out.println("Entonces vamos a registrarlo, ¿seria usted una persona humanao o juridica? h/j");
                while(true){
                    seleccion = scanner.nextLine().trim();
                    if(seleccion.equalsIgnoreCase("h") || seleccion.equalsIgnoreCase("j")){
                        break;
                    }
                    System.out.println("Elija una opcion valida");
                }
                if (seleccion.equalsIgnoreCase("h")){
                    Creador.crearPersonaHumana();
                } else {
                    Creador.crearPersonaJuridica();
                }
                System.out.println("Ahora que fue registrado, probemos nuevamente");
            } else {
                while(true){
                    System.out.println("Escriba el nombre con el que fue registrado");
                    GestorPersonas.getInstance();
                    persona = GestorPersonas.personaExiste(scanner.nextLine().trim());
                    if (persona == null){
                        System.out.println("Lo sentimos, no pudimos encontrar su usuario, ¿quiere intentar de nuevo? s/n");
                        while(true){
                            seleccion = scanner.nextLine().trim();
                            if(seleccion.equalsIgnoreCase("s") || seleccion.equalsIgnoreCase("n")){
                                break;
                            }
                            System.out.println("Elija una opcion valida");
                        }
                        if (seleccion.equalsIgnoreCase("n")){
                            System.out.println("Lamentamos mucho los inconvenientes, ¿Desea registrarse nuevamente? s/n");
                            while(true){
                                seleccion = scanner.nextLine().trim();
                                if(seleccion.equalsIgnoreCase("s") || seleccion.equalsIgnoreCase("n")){
                                    break;
                                }
                                System.out.println("Elija una opcion valida");
                            }
                            if(seleccion.equalsIgnoreCase("s")){
                                System.out.println("Entonces vamos que registrarlo, ¿Desea ser persona humana o juridica? h/n");
                                while(true){
                                    seleccion = scanner.nextLine().trim();
                                    if(seleccion.equalsIgnoreCase("h") || seleccion.equalsIgnoreCase("j")){
                                        break;
                                    }
                                    System.out.println("Elija una opcion valida");
                                }
                                if (seleccion.equalsIgnoreCase("h")){
                                    Creador.crearPersonaHumana();
                                } else {
                                    Creador.crearPersonaJuridica();
                                }
                                System.out.println("Ahora que fue registrado, probemos nuevamente");
                            } else {
                                System.out.println("Entendido, muchas gracias por intentar de todas maneras");
                                break;
                            }
                        }
                    } else {
                        System.out.println("Perfecto, su usuario fue encontrado");
                        System.out.println("Ahora empezemos a registrar los bienes que desea donar");
                        List<Donacion> donacionesSegmentadas = SegmentadorDonaciones.segmentar(persona, Creador.crearBienes());
                        for(Donacion donacion : donacionesSegmentadas){
                            System.out.println(donacion);
                            AsignadorDonaciones.getInstance();
                            AsignadorDonaciones.agregarDonacion(donacion);
                            AsignadorDonaciones.asignarDonacion(donacion);
                        }
                        System.out.println("Muchas gracias por su colaboracion, esperamos que siga colaborando con nosotros en el futuro");
                        break;
                    }
                }
                break;
            }
        }
    }

    static List<Bien> crearBienes(){
        String seleccion;
        String tipo;
        List<Bien> bienes = new ArrayList<>();
        while(true){
            System.out.println("¿Desea donar un bien con estado o perecedero? e/p");
            while(true){
                seleccion = scanner.nextLine().trim();
                if(seleccion.equalsIgnoreCase("e") || seleccion.equalsIgnoreCase("p")){
                    tipo = seleccion;
                    break;
                }
                System.out.println("Elija una opcion valida");
            }
            System.out.println("Escriba el nombre del bien");
            String nombre = scanner.nextLine().trim();
            System.out.println("Escriba a que categoria pertenece el bien");
            CategoriaBien categoria = CategoriaBien.valueOf(scanner.nextLine().trim().toUpperCase());
            System.out.println("Escriba una descripcion del bien");
            String descripcion = scanner.nextLine().trim();
            System.out.println("Escriba el url de una foto del bien");
            String urlFoto = scanner.nextLine().trim();
            System.out.println("Escriba la unidad de medida que se usa en el bien");
            UnidadDeMedida unidadDeMedida = UnidadDeMedida.valueOf(scanner.nextLine().trim().toUpperCase());
            System.out.println("Escriba la cantidad del bien segun la unidad de medida");
            Integer cantidad = scanner.nextInt();
            scanner.nextLine();
            if (tipo.equals("e")){
                System.out.println("¿Esta usado el bien? s/n");
                while(true){
                    seleccion = scanner.nextLine().trim();
                    if(seleccion.equalsIgnoreCase("s") || seleccion.equalsIgnoreCase("n")){
                        break;
                    }
                    System.out.println("Elija una opcion valida");
                }
                boolean usado = seleccion.equalsIgnoreCase("s");
                BienConEstado bien = new BienConEstado(descripcion, new SubcategoriaBien(nombre, categoria), urlFoto, cantidad, unidadDeMedida, usado);
                System.out.println(bien);
                bienes.add(bien);
            } else {
                System.out.println("Escriba el año de vencimiento del bien");
                int anio = scanner.nextInt();
                System.out.println("Escriba el mes de vencimiento del bien");
                int mes = scanner.nextInt();
                System.out.println("Escriba el dia de vencimiento del bien");
                int dia = scanner.nextInt();
                LocalDate fechaVencimiento = LocalDate.of(anio, mes, dia);
                BienPerecedero bien = new BienPerecedero(descripcion, new SubcategoriaBien(nombre, categoria), urlFoto, cantidad, unidadDeMedida, fechaVencimiento);
                System.out.println(bien);
                bienes.add(bien);
            }
            System.out.println("¿Desea agregar otro bien? s/n");
            while(true){
                seleccion = scanner.nextLine().trim();
                if(seleccion.equalsIgnoreCase("s") || seleccion.equalsIgnoreCase("n")){
                    break;
                }
                System.out.println("Elija una opcion valida");
            }
            if (seleccion.equalsIgnoreCase("n")){
                break;
            }
        }
        return bienes;
    }
}
