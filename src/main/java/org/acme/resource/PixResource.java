package org.acme.resource;

import java.math.BigDecimal;
import java.util.List;

import org.acme.dto.PixCobrancaDTO;
import org.acme.model.PixImediato;
import org.acme.service.PixService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint REST para operações relacionadas ao Pix
 */
@Path("/pix")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PixResource {

    private static final Logger LOG = Logger.getLogger(PixResource.class);

    @Inject
    PixService pixService;

    /**
     * Cria uma nova cobrança Pix
     * 
     * @param pixData Dados da cobrança no formato JSON
     * @return Resposta com os detalhes da cobrança criada
     */
    @POST
    @Path("/cobranca")
    @Operation(summary = "Cria uma nova cobrança Pix", description = "Este endpoint cria uma nova cobrança Pix com os dados fornecidos. "
            +
            "Uma cobrança Pix permite receber pagamentos instantâneos através do sistema Pix brasileiro. " +
            "Retorna um código QR e um Pix Copia e Cola que podem ser usados para efetuar o pagamento.")
    @APIResponse(responseCode = "201", description = "Cobrança criada com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "400", description = "Dados inválidos para criar a cobrança", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response criarCobranca(PixCobrancaDTO pixData) {
        try {
            LOG.info("Recebendo solicitação para criar cobrança Pix");

            // Extrair dados do DTO
            String txid = pixService.gerarTxid();
            String chave = pixData.chave();
            BigDecimal valor = new BigDecimal(pixData.valor());
            String nome = pixData.nome();
            String cpf = pixData.cpf();
            String cnpj = pixData.cnpj();
            Integer expiracao = pixData.expiracao();

            // Validar dados obrigatórios
            if (chave == null || valor.compareTo(BigDecimal.ZERO) <= 0 || nome == null) {
                LOG.warn("Requisição inválida: campos obrigatórios ausentes");
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new JsonObject().put("erro", "Chave Pix, valor e nome são obrigatórios").encode())
                        .build();
            }

            // Criar objeto PixImediato
            PixImediato pixImediato = new PixImediato(txid, chave, valor, nome, cpf, cnpj, expiracao);

            // Adicionar solicitação ao pagador se existir
            if (pixData.solicitacaoPagador() != null && !pixData.solicitacaoPagador().isEmpty()) {
                pixImediato.setSolicitacaoPagador(pixData.solicitacaoPagador());
            }

            // Adicionar informações adicionais
            // if (pixData.infoAdicionais() != null && !pixData.infoAdicionais().isEmpty()) {
            //     JsonArray infoArray = new JsonArray(pixData.infoAdicionais());
            //     for (int i = 0; i < infoArray.size(); i++) {
            //         JsonObject info = infoArray.getJsonObject(i);
            //         pixImediato.addInfoAdicional(info.getString("nome"), info.getString("valor"));
            //     }
            // }

            // Criar cobrança PIX
            JsonObject resultado = pixService.criarCobrancaPix(pixImediato);

            // Adicionar dados de retorno
            JsonObject resposta = new JsonObject();
            resposta.put("txid", pixImediato.getTxid());
            resposta.put("status", pixImediato.getStatus());
            resposta.put("pixCopiaECola", pixImediato.getPixCopiaECola());
            resposta.put("qrCode", resultado.getString("pixCopiaECola"));
            resposta.put("location", pixImediato.getLocation());
            resposta.put("criacao", pixImediato.getCriacao().toString());
            resposta.put("expiracao", pixImediato.getExpiracao());

            return Response.status(Response.Status.CREATED).entity(resposta.encode()).build();

        } catch (Exception e) {
            LOG.error("Erro ao criar cobrança Pix", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Cancela uma cobrança Pix existente
     * 
     * @param txid ID da transação
     * @return Resposta com o resultado da operação
     */
    @DELETE
    @Path("/cobranca/{txid}")
    @Operation(
        summary = "Cancela uma cobrança Pix existente",
        description = "Este endpoint cancela uma cobrança Pix que ainda não foi paga. " +
                      "Uma vez cancelada, a cobrança não poderá mais receber pagamentos e seu status será alterado para REMOVIDA_PELO_USUARIO_RECEBEDOR. " +
                      "Não é possível cancelar cobranças que já foram pagas."
    )
    @APIResponse(
        responseCode = "200", 
        description = "Cobrança cancelada com sucesso",
        content = @Content(mediaType = "application/json")
    )
    @APIResponse(
        responseCode = "500", 
        description = "Erro interno ao processar a requisição",
        content = @Content(mediaType = "application/json")
    )
    @APIResponse(
        responseCode = "404", 
        description = "Cobrança não encontrada",
        content = @Content(mediaType = "application/json")
    )
    @APIResponse(
        responseCode = "409", 
        description = "Cobrança já foi paga e não pode ser cancelada",
        content = @Content(mediaType = "application/json")
    )
    @APIResponse(
        responseCode = "500", 
        description = "Erro interno ao processar a requisição",
        content = @Content(mediaType = "application/json")
    )
    public Response cancelarCobranca(@PathParam("txid") String txid) {
        try {
            LOG.info("Cancelando cobrança Pix: " + txid);
            PixImediato pixImediato = pixService.consultarCobranca(txid);

            if (pixImediato == null) {
                LOG.warn("Cobrança não encontrada para cancelamento: " + txid);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new JsonObject().put("erro", "Cobrança não encontrada").encode())
                        .build();
            }

            // Verificar se a cobrança já foi paga
            if (pixImediato.isPaga()) {
                LOG.warn("Tentativa de cancelar cobrança já paga: " + txid);
                return Response.status(Response.Status.CONFLICT)
                        .entity(new JsonObject().put("erro", "Não é possível cancelar uma cobrança já paga").encode())
                        .build();
            }

            // Cancelar a cobrança
            pixImediato.cancelar();
            pixService.persistirCobranca(pixImediato);

            return Response.ok(new JsonObject()
                    .put("txid", txid)
                    .put("status", "REMOVIDA_PELO_USUARIO_RECEBEDOR")
                    .put("mensagem", "Cobrança cancelada com sucesso")
                    .encode()).build();

        } catch (Exception e) {
            LOG.error("Erro ao cancelar cobrança Pix", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Gera QR Code para uma cobrança existente
     * 
     * @param txid ID da transação
     * @return Imagem do QR Code em base64
     */
    @GET
    @Path("/cobranca/{txid}/qrcode")
    @Produces("image/png")
    @Operation(summary = "Gera imagem QR Code para uma cobrança Pix", description = "Este endpoint gera uma imagem PNG do QR Code para uma cobrança Pix existente. "
            +
            "O QR Code pode ser escaneado por aplicativos bancários para efetuar o pagamento. " +
            "É necessário que a cobrança tenha um Pix Copia e Cola válido para gerar o QR Code.")
    @APIResponse(responseCode = "200", description = "QR Code gerado com sucesso", content = @Content(mediaType = "image/png"))
    @APIResponse(responseCode = "404", description = "Cobrança não encontrada", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "400", description = "Dados para QR Code não disponíveis", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response gerarQrCode(@PathParam("txid") String txid) {
        try {
            LOG.info("Gerando QR Code para cobrança: " + txid);
            PixImediato pixImediato = pixService.consultarCobranca(txid);

            if (pixImediato == null) {
                LOG.warn("Cobrança não encontrada para gerar QR Code: " + txid);
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            // Verificar se o Pix Copia e Cola está disponível
            if (pixImediato.getPixCopiaECola() == null || pixImediato.getPixCopiaECola().isEmpty()) {
                LOG.warn("Dados para QR Code não disponíveis: " + txid);
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new JsonObject().put("erro", "Dados para QR Code não disponíveis").encode())
                        .type(MediaType.APPLICATION_JSON)
                        .build();
            }

            // Gerar imagem do QR Code
            byte[] qrCodeImage = pixService.gerarQrCodeImage(pixImediato.getPixCopiaECola());

            return Response.ok(qrCodeImage).build();

        } catch (Exception e) {
            LOG.error("Erro ao gerar QR Code", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    /**
     * Registra o pagamento de uma cobrança Pix (simulação)
     * 
     * @param txid           ID da transação
     * @param dadosPagamento Dados do pagamento
     * @return Resultado da operação
     */
    @POST
    @Path("/cobranca/{txid}/pagar")
    @Operation(summary = "Registra o pagamento de uma cobrança Pix (simulação)", description = "Este endpoint simula o recebimento de um pagamento Pix para uma cobrança existente. "
            +
            "É utilizado apenas para fins de teste e simulação, já que em um ambiente real o pagamento seria " +
            "confirmado automaticamente pelo PSP (Provedor de Serviços de Pagamento). " +
            "Altera o status da cobrança para CONCLUIDA e registra os detalhes do pagamento.")
    @APIResponse(responseCode = "200", description = "Pagamento registrado com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "404", description = "Cobrança não encontrada", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "409", description = "Cobrança já foi paga", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response registrarPagamento(@PathParam("txid") String txid, JsonObject dadosPagamento) {
        try {
            LOG.info("Registrando pagamento para cobrança: " + txid);
            PixImediato pixImediato = pixService.consultarCobranca(txid);

            if (pixImediato == null) {
                LOG.warn("Cobrança não encontrada para pagamento: " + txid);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new JsonObject().put("erro", "Cobrança não encontrada").encode())
                        .build();
            }

            // Verificar se a cobrança já foi paga
            if (pixImediato.isPaga()) {
                LOG.warn("Cobrança já foi paga: " + txid);
                return Response.status(Response.Status.CONFLICT)
                        .entity(new JsonObject().put("erro", "Cobrança já foi paga").encode())
                        .build();
            }

            // Extrair dados do pagamento
            String endToEndId = dadosPagamento.getString("endToEndId", "E" + System.currentTimeMillis());
            BigDecimal valorPago = new BigDecimal(
                    dadosPagamento.getString("valorPago", pixImediato.getValorOriginal().toString()));
            String infoPagador = dadosPagamento.getString("infoPagador", "");

            // Registrar o pagamento
            pixImediato.registrarPagamento(endToEndId, valorPago, infoPagador);
            pixService.persistirCobranca(pixImediato);

            JsonObject resposta = new JsonObject()
                    .put("txid", txid)
                    .put("status", "CONCLUIDA")
                    .put("valorPago", valorPago.toString())
                    .put("endToEndId", endToEndId)
                    .put("horarioPagamento", pixImediato.getHorarioPagamento().toString());

            return Response.ok(resposta.encode()).build();

        } catch (Exception e) {
            LOG.error("Erro ao registrar pagamento", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Converte um objeto PixImediato em um objeto JSON para retorno na API
     * 
     * @param pix Objeto PixImediato a ser convertido
     * @return Objeto JSON com os dados da cobrança
     */
    private JsonObject criarJsonDePix(PixImediato pix) {
        JsonObject json = new JsonObject();

        json.put("txid", pix.getTxid());
        json.put("status", pix.getStatus());
        json.put("chave", pix.getChave());
        json.put("valor", pix.getValorOriginal().toString());
        json.put("nome", pix.getNome());

        if (pix.getCpf() != null && !pix.getCpf().isEmpty()) {
            json.put("cpf", pix.getCpf());
        }

        if (pix.getCnpj() != null && !pix.getCnpj().isEmpty()) {
            json.put("cnpj", pix.getCnpj());
        }

        json.put("criacao", pix.getCriacao().toString());
        json.put("expiracao", pix.getExpiracao());

        if (pix.getPixCopiaECola() != null) {
            json.put("pixCopiaECola", pix.getPixCopiaECola());
        }

        if (pix.getLocation() != null) {
            json.put("location", pix.getLocation());
        }

        if (pix.isPaga()) {
            json.put("pago", true);
            json.put("horarioPagamento", pix.getHorarioPagamento().toString());
            json.put("valorPago", pix.getValorPago().toString());
        } else {
            json.put("pago", false);
        }

        if (pix.getSolicitacaoPagador() != null) {
            json.put("solicitacaoPagador", pix.getSolicitacaoPagador());
        }

        return json;
    }

    /**
     * Consulta uma cobrança Pix pelo TxID
     * 
     * @param txid ID da transação
     * @return Detalhes da cobrança se encontrada
     */
    @GET
    @Path("/cobranca/{txid}")
    @Operation(summary = "Consulta uma cobrança Pix pelo TxID", description = "Este endpoint consulta uma cobrança Pix específica pelo seu identificador de transação (TxID). "
            +
            "Retorna os detalhes completos da cobrança, incluindo status, valor, dados do pagador e, " +
            "se já paga, informações do pagamento. A consulta é feita no banco de dados local, não na API do PSP.")
    @APIResponse(responseCode = "200", description = "Cobrança encontrada com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "404", description = "Cobrança não encontrada", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response consultarCobranca(@PathParam("txid") String txid) {
        try {
            LOG.info("Consultando cobrança Pix: " + txid);
            PixImediato pixImediato = pixService.consultarCobranca(txid);

            if (pixImediato == null) {
                LOG.warn("Cobrança não encontrada: " + txid);
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new JsonObject().put("erro", "Cobrança não encontrada").encode())
                        .build();
            }

            // Transformar objeto em JSON
            JsonObject resultado = criarJsonDePix(pixImediato);

            return Response.ok(resultado.encode()).build();

        } catch (Exception e) {
            LOG.error("Erro ao consultar cobrança Pix", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Consulta uma cobrança Pix diretamente na API do banco
     * 
     * @param txid ID da transação
     * @return Detalhes atualizados da cobrança
     */
    @GET
    @Path("/cobranca/{txid}/atualizar")
    @Operation(summary = "Atualiza informações de uma cobrança Pix com dados da API do banco", description = "Este endpoint consulta uma cobrança Pix diretamente na API do PSP/banco e atualiza o registro local. "
            +
            "É útil para sincronizar o status e outras informações da cobrança quando ocorreram atualizações no PSP " +
            "que ainda não foram refletidas localmente, como confirmações de pagamento.")
    @APIResponse(responseCode = "200", description = "Cobrança atualizada com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response atualizarCobranca(@PathParam("txid") String txid) {
        try {
            LOG.info("Atualizando cobrança Pix da API: " + txid);
            JsonObject resultado = pixService.consultarCobrancaNoServidor(txid);

            return Response.ok(resultado.encode()).build();

        } catch (Exception e) {
            LOG.error("Erro ao atualizar cobrança Pix", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Lista as cobranças mais recentes
     * 
     * @return Lista de cobranças
     */
    @GET
    @Path("/cobrancas")
    @Operation(summary = "Lista as cobranças Pix mais recentes", description = "Este endpoint retorna uma lista das cobranças Pix mais recentes criadas pelo sistema. "
            +
            "É possível limitar a quantidade de resultados através do parâmetro de consulta 'limite'. " +
            "A lista é ordenada pela data de criação, com as cobranças mais recentes primeiro.")
    @APIResponse(responseCode = "200", description = "Lista de cobranças obtida com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response listarCobrancas(@QueryParam("limite") @DefaultValue("10") int limite) {
        try {
            LOG.info("Listando " + limite + " cobranças Pix");
            List<PixImediato> cobrancas = pixService.listarCobrancasRecentes(limite);

            // Transformar lista em array JSON
            JsonObject resultado = new JsonObject();
            JsonArray listaCobrancas = new JsonArray();

            for (PixImediato pix : cobrancas) {
                listaCobrancas.add(criarJsonDePix(pix));
            }

            resultado.put("quantidade", cobrancas.size());
            resultado.put("cobrancas", listaCobrancas);

            return Response.ok(resultado.encode()).build();

        } catch (Exception e) {
            LOG.error("Erro ao listar cobranças Pix", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Consulta detalhes de cobranças Pix usando consulta direta na API do banco
     * 
     * @param txid ID da transação
     * @return Detalhes completos da cobrança com informações de pagamento
     */
    @GET
    @Path("/cobranca/{txid}/detalhes")
    @Operation(summary = "Consulta detalhes completos de uma cobrança Pix direto da API do banco", description = "Este endpoint consulta detalhes completos de uma cobrança Pix diretamente na API do PSP/banco. "
            +
            "Diferente da consulta regular, este endpoint retorna todas as informações disponíveis na API, " +
            "incluindo dados detalhados sobre pagamentos e devoluções. É a forma mais completa de obter " +
            "informações atualizadas sobre uma cobrança Pix.")
    @APIResponse(responseCode = "200", description = "Detalhes da cobrança obtidos com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "400", description = "Erro na resposta da API do banco", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response consultarDetalhesCobranca(@PathParam("txid") String txid) {
        try {
            LOG.info("Consultando detalhes completos de cobrança Pix: " + txid);
            JsonObject resultado = pixService.consultarDetalhesCobranca(txid);

            // Verificar se a resposta é um erro da API
            if (resultado.containsKey("type") && resultado.containsKey("title") && resultado.containsKey("status")) {
                LOG.warn("Erro na consulta de detalhes: " + resultado.getString("title"));
                return Response.status(resultado.getInteger("status", 400))
                        .entity(resultado.encode())
                        .build();
            }

            return Response.ok(resultado.encode()).build();

        } catch (Exception e) {
            LOG.error("Erro ao consultar detalhes da cobrança Pix", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject()
                            .put("erro", "Falha na consulta de detalhes")
                            .put("mensagem", e.getMessage())
                            .encode())
                    .build();
        }
    }

    /**
     * Verifica o status do pagamento de uma cobrança Pix
     * 
     * @param txid ID da transação
     * @return Status do pagamento e detalhes se pago
     */
    @GET
    @Path("/cobranca/{txid}/status")
    @Operation(summary = "Verifica o status de pagamento de uma cobrança Pix", description = "Este endpoint verifica o status atual de pagamento de uma cobrança Pix consultando a API do PSP/banco. "
            +
            "Retorna um objeto simplificado contendo o status da cobrança (ATIVA, CONCLUIDA, etc.), se foi paga ou não, "
            +
            "e detalhes do pagamento quando realizado. É útil para verificar rapidamente se um Pix foi recebido.")
    @APIResponse(responseCode = "200", description = "Status do pagamento obtido com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "404", description = "Cobrança não encontrada ou não foi paga", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response verificarStatusPagamento(@PathParam("txid") String txid) {
        try {
            LOG.info("Verificando status de pagamento da cobrança: " + txid);
            JsonObject statusPagamento = pixService.verificarStatusPagamento(txid);

            return Response.ok(statusPagamento.encode()).build();

        } catch (Exception e) {
            LOG.error("Erro ao verificar status de pagamento", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Consulta se existe devolução para um Pix específico
     * 
     * @param txid ID da transação
     * @return Detalhes da devolução se existir
     */
    @GET
    @Path("/cobranca/{txid}/devolucao")
    @Operation(summary = "Consulta informações de devolução para uma cobrança Pix", description = "Este endpoint verifica se existe alguma devolução associada a um pagamento Pix. "
            +
            "As devoluções ocorrem quando um pagamento Pix já realizado precisa ser estornado, seja por " +
            "solicitação do pagador, problemas com o produto/serviço, ou outras razões. " +
            "Retorna detalhes da devolução quando existente, incluindo valor, motivo e status.")
    @APIResponse(responseCode = "200", description = "Informações de devolução obtidas com sucesso", content = @Content(mediaType = "application/json"))
    @APIResponse(responseCode = "500", description = "Erro interno ao processar a requisição", content = @Content(mediaType = "application/json"))
    public Response consultarDevolucao(@PathParam("txid") String txid) {
        try {
            LOG.info("Consultando devolução da cobrança: " + txid);
            JsonObject resultado = pixService.consultarDevolucao(txid);

            return Response.ok(resultado.encode()).build();

        } catch (Exception e) {
            LOG.error("Erro ao consultar devolução", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }
}