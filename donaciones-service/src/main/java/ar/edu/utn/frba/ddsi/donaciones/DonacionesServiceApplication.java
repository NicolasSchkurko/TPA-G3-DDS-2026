package ar.edu.utn.frba.ddsi.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.Personas.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
public class DonacionesServiceApplication {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int opcion;
        String seleccion;

        System.out.println("Elija una opcion");
        while(true){
            System.out.println("1. Registrar persona donante");
            System.out.println("2. Registrar entidad beneficiaria");
            System.out.println("3. Registrar donacion");
            System.out.println("4. Agregar una nueva necesidad");
            opcion = scanner.nextInt();
            scanner.nextLine();
            switch(opcion){
                case 1:
                    System.out.println("Vamos a registrar a una persona, ¿es esta una persona humana o juridica? h/j");
                    while(true){
                        seleccion = scanner.nextLine().trim();
                        if(seleccion.equalsIgnoreCase("h") || seleccion.equalsIgnoreCase("j")){
                            break;
                        }
                        System.out.println("Elija una opcion valida");
                    }
                    if (seleccion.equalsIgnoreCase("h")){
                        Creador.crearPersonaHumana();
                    } else{
                        Creador.crearPersonaJuridica();
                    }
                    break;
                case 2:
                    Creador.crearEntidad();
                    break;
                case 3:
                    Creador.crearDonaciones();
                    break;
                default:
                    System.out.println("Opción inexistente, seleccione de nuevo");
                    continue;
            }
            System.out.println("¿Desea hacer algo más? s/n");
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
        System.out.println("Muchas gracias, que tenga un buen dia");
        SpringApplication.run(DonacionesServiceApplication.class, args);
    }
}
