package ar.edu.utn.frba.ddsi.logisticas.RabbitMQ;

import ar.edu.utn.frba.ddsi.logisticas.config.RabbitMQConfig;
import ar.edu.utn.frba.ddsi.logisticas.dto.entrega.EntregaDTO;
import ar.edu.utn.frba.ddsi.logisticas.services.EntregaService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DonacionListener {
    private final EntregaService entregaService;

    public DonacionListener(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @RabbitListener(queues = RabbitMQConfig.NUEVAS_DONACIONES_QUEUE)
    public void recibirDonacionParaEntregar(EntregaDTO peticion) {
        try {
            entregaService.procesarPeticion(peticion);
        } catch (Exception e) {
            System.err.println("Error al procesar mensaje desde RabbitMQ: " + e.getMessage());
        }
    }
}