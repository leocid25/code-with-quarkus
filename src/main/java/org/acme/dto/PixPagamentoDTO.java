package org.acme.dto;

import java.math.BigDecimal;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO que representa os dados de um pagamento Pix
 */
@Schema(description = "Dados para registrar pagamento de uma cobrança Pix")
public record PixPagamentoDTO(
        @Schema(description = "Identificador único da transação de pagamento", example = "E2023021012345678", required = false)
        String endToEndId,

        @Schema(description = "Valor efetivamente pago", example = "100.00", required = true)
        BigDecimal valorPago,

        @Schema(description = "Informação adicional enviada pelo pagador", example = "Pagamento referente ao pedido #12345", required = false) String infoPagador) {
    // Não é necessário implementar construtor, getters ou setters em um record
    // Java faz isso automaticamente

    @Override
    public String toString() {
        return "PixPagamentoDTO[" +
                "endToEndId=" + endToEndId + ", " +
                "valorPago=" + valorPago + ", " +
                "infoPagador=" + infoPagador + "]";
    }
}