package org.acme.resource;

import java.time.LocalDateTime;

import org.acme.model.TokenBB;
import org.acme.service.TokenService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Endpoint REST para gerenciamento de tokens de acesso
 */
@Path("/token")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Token", description = "Operações relacionadas a tokens de acesso para APIs")
public class TokenResource {

    private static final Logger LOG = Logger.getLogger(TokenResource.class);

    @Inject
    TokenService tokenService;

    /**
     * Obtém o token de acesso atual ou gera um novo
     * 
     * @return Resposta com os detalhes do token
     */
    @GET
    @Operation(
        summary = "Obtém um token de acesso válido",
        description = "Este endpoint retorna um token de acesso válido para a API do Banco do Brasil. " +
                      "Se houver um token válido no banco de dados, ele será retornado. " +
                      "Caso contrário, um novo token será solicitado à API do banco. " +
                      "O token é necessário para realizar operações Pix."
    )
    @APIResponse(
        responseCode = "200", 
        description = "Token obtido com sucesso",
        content = @Content(mediaType = "application/json")
    )
    @APIResponse(
        responseCode = "500", 
        description = "Erro interno ao processar a requisição",
        content = @Content(mediaType = "application/json")
    )
    public Response obterToken() {
        try {
            LOG.info("Solicitando token de acesso");
            
            // Obter token usando o serviço
            String accessToken = tokenService.getAccessToken();
            
            // Obter informações do token atual para retornar ao cliente
            TokenBB token = tokenService.getTokenInfo();
            
            // Criar resposta
            JsonObject resposta = new JsonObject();
            resposta.put("token", accessToken);
            
            if (token != null) {
                resposta.put("expiresAt", token.getExpiresAt().toString());
                
                // Calcular tempo restante em minutos
                long minutosRestantes = java.time.Duration.between(
                    LocalDateTime.now(), token.getExpiresAt()).toMinutes();
                resposta.put("expiresInMinutes", minutosRestantes);
            }
            
            return Response.ok(resposta.encode()).build();
            
        } catch (Exception e) {
            LOG.error("Erro ao obter token de acesso", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }

    /**
     * Força a renovação do token, descartando qualquer token válido existente
     * 
     * @return Resposta com os detalhes do novo token
     */
    @POST
    @Path("/renovar")
    @Operation(
        summary = "Força a renovação do token de acesso",
        description = "Este endpoint força a renovação do token de acesso, descartando qualquer token válido existente. " +
                      "Um novo token será solicitado à API do banco independentemente da validade do token atual. " +
                      "Útil quando o token atual apresenta problemas ou quando se deseja garantir um token recém-emitido."
    )
    @APIResponse(
        responseCode = "200", 
        description = "Token renovado com sucesso",
        content = @Content(mediaType = "application/json")
    )
    @APIResponse(
        responseCode = "500", 
        description = "Erro interno ao processar a requisição",
        content = @Content(mediaType = "application/json")
    )
    public Response renovarToken() {
        try {
            LOG.info("Forçando renovação do token de acesso");
            
            // Solicitar novo token ignorando token existente
            String accessToken = tokenService.renovarToken();
            
            // Obter informações do novo token
            TokenBB token = tokenService.getTokenInfo();
            
            // Criar resposta
            JsonObject resposta = new JsonObject();
            resposta.put("token", accessToken);
            resposta.put("renovado", true);
            
            if (token != null) {
                resposta.put("expiresAt", token.getExpiresAt().toString());
                
                // Calcular tempo restante em minutos
                long minutosRestantes = java.time.Duration.between(
                    LocalDateTime.now(), token.getExpiresAt()).toMinutes();
                resposta.put("expiresInMinutes", minutosRestantes);
            }
            
            return Response.ok(resposta.encode()).build();
            
        } catch (Exception e) {
            LOG.error("Erro ao renovar token de acesso", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }
    
    /**
     * Verifica o status atual do token
     * 
     * @return Resposta com informações sobre o estado do token
     */
    @GET
    @Path("/status")
    @Operation(
        summary = "Verifica o status do token atual",
        description = "Este endpoint verifica o status do token de acesso atual no banco de dados. " +
                      "Retorna informações como validade, tempo de expiração, e se é necessário renová-lo. " +
                      "Útil para monitoramento e diagnóstico da autenticação com a API do banco."
    )
    @APIResponse(
        responseCode = "200", 
        description = "Status do token verificado com sucesso",
        content = @Content(mediaType = "application/json")
    )
    @APIResponse(
        responseCode = "500", 
        description = "Erro interno ao processar a requisição",
        content = @Content(mediaType = "application/json")
    )
    public Response verificarStatusToken() {
        try {
            LOG.info("Verificando status do token atual");
            
            // Obter informações do token atual
            TokenBB token = tokenService.getTokenInfo();
            
            // Criar resposta
            JsonObject resposta = new JsonObject();
            
            if (token == null) {
                resposta.put("status", "AUSENTE");
                resposta.put("mensagem", "Não há token armazenado no banco de dados");
                resposta.put("precisaRenovar", true);
            } else {
                boolean expirado = tokenService.isTokenExpired(token);
                resposta.put("status", expirado ? "EXPIRADO" : "VÁLIDO");
                resposta.put("token", token.getAccessToken());
                resposta.put("criadoEm", token.getCreatedAt().toString());
                resposta.put("expiraEm", token.getExpiresAt().toString());
                
                // Calcular tempo restante em minutos
                long minutosRestantes = java.time.Duration.between(
                    LocalDateTime.now(), token.getExpiresAt()).toMinutes();
                resposta.put("minutosRestantes", minutosRestantes);
                resposta.put("precisaRenovar", expirado);
            }
            
            return Response.ok(resposta.encode()).build();
            
        } catch (Exception e) {
            LOG.error("Erro ao verificar status do token", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new JsonObject().put("erro", e.getMessage()).encode())
                    .build();
        }
    }
}
