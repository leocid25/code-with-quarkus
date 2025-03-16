package org.acme.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

import org.acme.config.PixConfig;
import org.acme.model.TokenBB;
import org.acme.repository.TokenRepository;
import org.jboss.logging.Logger;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Serviço responsável pelo gerenciamento de tokens de acesso
 * para integração com a API PIX do Banco do Brasil
 */
@ApplicationScoped
public class TokenService {

    private static final Logger LOG = Logger.getLogger(TokenService.class);
    
    @Inject
    TokenRepository tokenRepository;
    
    @Inject
    PixConfig pixConfig;

    /**
     * Obtém um token de acesso válido
     * Primeiro verifica se existe um token válido no banco de dados
     * Se não existir ou estiver expirado, solicita um novo token
     * 
     * @return Token de acesso válido
     * @throws Exception Se não for possível obter um token
     */
    @Transactional
    public String getAccessToken() throws Exception {
        try {
            // Buscar token válido diretamente usando o repositório
            // Adiciona uma margem de segurança de 5 minutos
            LocalDateTime timeWithMargin = LocalDateTime.now().plusMinutes(5);
            TokenBB token = tokenRepository.findValidToken(timeWithMargin);
            
            if (token != null) {
                LOG.debug("Utilizando token existente válido até: " + token.getExpiresAt());
                return token.getAccessToken();
            } else {
                LOG.info("Nenhum token válido encontrado. Solicitando novo token...");
                return requestNewToken();
            }
        } catch (Exception e) {
            LOG.error("Erro ao obter token de acesso", e);
            throw e;
        }
    }
    
    /**
     * Força a renovação do token, ignorando qualquer token válido existente
     * 
     * @return Novo token de acesso
     * @throws Exception Se não for possível obter um novo token
     */
    @Transactional
    public String renovarToken() throws Exception {
        LOG.info("Forçando renovação de token de acesso");
        return requestNewToken();
    }
    
    /**
     * Retorna informações sobre o token atual
     * 
     * @return O objeto TokenBB atual ou null se não existir
     */
    public TokenBB getTokenInfo() {
        try {
            return tokenRepository.findMostRecentToken();
        } catch (Exception e) {
            LOG.error("Erro ao buscar informações do token", e);
            return null;
        }
    }
    
    /**
     * Verifica se um token está expirado, considerando margem de segurança
     * 
     * @param token O token a ser verificado
     * @return true se o token estiver expirado, false caso contrário
     */
    public boolean isTokenExpired(TokenBB token) {
        // Adicionando margem de segurança de 5 minutos
        return tokenRepository.isTokenExpired(token, LocalDateTime.now().plusMinutes(5));
    }

    /**
     * Solicita um novo token de acesso à API do Banco do Brasil
     * 
     * @return O token de acesso obtido
     * @throws Exception Se não for possível obter o token
     */
    @Transactional
    protected String requestNewToken() throws Exception {
        // Criar o cliente HTTP
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Preparar o corpo da requisição
        String requestBody = "grant_type=client_credentials";

        // Criar a autorização Basic para o header
        String auth = pixConfig.getClientId() + ":" + pixConfig.getClientSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        // Construir a requisição HTTP
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(pixConfig.getTokenUrl()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + encodedAuth)
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Enviar a requisição e obter a resposta
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Processar a resposta
        if (response.statusCode() == 200) {
            JsonObject jsonResponse = new JsonObject(response.body());
            
            // Extrair dados do token
            String accessToken = jsonResponse.getString("access_token");
            int expiresIn = jsonResponse.getInteger("expires_in", 3600); // Padrão 1 hora
            
            LOG.info("Token obtido com sucesso. Válido por " + expiresIn + " segundos");
            
            // Salvar o token no banco de dados
            TokenBB novoToken = saveToken(accessToken, expiresIn);
            
            // Limpar tokens antigos do banco de dados
            limparTokensAntigos(novoToken);
            
            return accessToken;
        } else {
            LOG.error("Falha na autenticação. Código: " + response.statusCode() + ", Resposta: " + response.body());
            throw new RuntimeException("Falha na autenticação. Código: " + response.statusCode() + ", Resposta: " + response.body());
        }
    }
    
    /**
     * Salva um novo token no banco de dados
     * 
     * @param accessToken Token de acesso
     * @param expiresIn Tempo de expiração em segundos
     * @return O objeto TokenBB criado e salvo
     */
    private TokenBB saveToken(String accessToken, int expiresIn) {
        TokenBB token = new TokenBB();
        token.setAccessToken(accessToken);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
        
        tokenRepository.saveToken(token);
        LOG.debug("Token salvo no banco de dados. Expira em: " + token.getExpiresAt());
        
        return token;
    }
    
    /**
     * Remove tokens antigos do banco de dados
     * 
     * @param tokenAtual Token atual que deve ser mantido
     */
    @Transactional
    public void limparTokensAntigos(TokenBB tokenAtual) {
        try {
            long removidos = tokenRepository.deleteOldTokens(tokenAtual);
            if (removidos > 0) {
                LOG.info("Tokens antigos removidos do banco de dados: " + removidos);
            }
        } catch (Exception e) {
            LOG.error("Erro ao limpar tokens antigos", e);
            // Não propagamos o erro para não afetar o fluxo principal
        }
    }
    
    /**
     * Limpa tokens expirados do banco de dados
     * Útil para execução programada (job/scheduler)
     * 
     * @return Número de tokens removidos
     */
    @Transactional
    public long limparTokensExpirados() {
        try {
            LocalDateTime agora = LocalDateTime.now();
            long removidos = tokenRepository.delete("expiresAt < ?1", agora);
            
            if (removidos > 0) {
                LOG.info("Tokens expirados removidos do banco de dados: " + removidos);
            }
            
            return removidos;
        } catch (Exception e) {
            LOG.error("Erro ao limpar tokens expirados", e);
            return 0;
        }
    }
}