package ar.edu.utn.frba.ddsi.incentivos.services;

import ar.edu.utn.frba.ddsi.incentivos.models.repositories.RepositorioPerfiles;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {
    private final RepositorioPerfiles repositorioPerfiles = RepositorioPerfiles.getInstance();


}
