package ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil;

import ar.edu.utn.frba.ddsi.incentivos.models.entities.Insignias.Insignia;
import ar.edu.utn.frba.ddsi.incentivos.models.entities.Mision.Mision;

import java.util.ArrayList;
import java.util.List;

//intento consumo de api
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static ar.edu.utn.frba.ddsi.incentivos.models.entities.Perfil.Categoria.*;

public class Perfil {
    //perfil guarda el id_personaDonante como key para api_rest
    //y atributos que necesite. Para conseguir la persDon se debe ir a la api
    //asi no hay 2 PersonaDonante
    //TODO agregar los atributos necesarios que vengan del repositorio
    private Long id_personaDonante;
    private final HttpClient client;
    private final ObjectMapper mapper;
    //me parece que ira en otra clase cuando usemos api
//    private Categoria categoria;
//    private List<Insignia> insignias;
//    private Mision misionActual;

    public Perfil() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }



//    public Perfil(PersonaDonante persona){
//        this.persona = persona;
//
//        this.insignias = new ArrayList<>();
//    }
}
