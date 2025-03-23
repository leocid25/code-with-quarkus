package org.acme.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO para resposta após registrar um pagamento Pix
 */
@Schema(description = "Resposta após registrar um pagamento Pix")
public record PixPagamentoResponseDTO(
        @Schema(description = "ID da transação Pix", example = "PixID123456789", required = true)
        String txid,

        @Schema(description = "Status após o pagamento", example = "CONCLUIDA", required = true)
        String status,

        @Schema(description = "Valor que foi pago", example = "100.00", required = true)
        BigDecimal valorPago,

        @Schema(description = "Identificador da transação de pagamento", example = "E2023021012345678", required = true)
        String endToEndId,

        @Schema(description = "Data e hora do pagamento", example = "2023-02-10T14:30:00", required = true)
        LocalDateTime horarioPagamento) {
    // Não é necessário implementar construtor, getters ou setters em um record
}
