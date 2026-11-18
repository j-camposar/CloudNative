package com.kafka.pago.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kafka.pago.Model.EstadoPagoCliente;
@Repository
public interface EstadoPagoClienteRepositorio extends JpaRepository<EstadoPagoCliente, String> {
}
