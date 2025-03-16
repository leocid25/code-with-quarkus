package org.acme.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Classe de configuração para os parâmetros da API Pix
 */
@ApplicationScoped
public class PixConfig {
    
    @ConfigProperty(name = "pix.banco-brasil.client-id")
    String clientId;
    
    @ConfigProperty(name = "pix.banco-brasil.client-secret")
    String clientSecret;
    
    @ConfigProperty(name = "pix.banco-brasil.app-key")
    String appKey;
    
    @ConfigProperty(name = "pix.ambiente")
    String ambiente;
    
    @ConfigProperty(name = "pix.banco-brasil.token-url")
    String tokenUrl;
    
    @ConfigProperty(name = "pix.banco-brasil.pix-url")
    String pixUrl;
    
    /**
     * Verifica se o ambiente é de produção
     * 
     * @return true se for produção, false caso contrário
     */
    public boolean isProducao() {
        return "producao".equalsIgnoreCase(ambiente);
    }
    
    /**
     * Obtém o cliente ID para autenticação
     * 
     * @return Cliente ID
     */
    public String getClientId() {
        return clientId;
    }
    
    /**
     * Obtém o cliente secret para autenticação
     * 
     * @return Cliente secret
     */
    public String getClientSecret() {
        return clientSecret;
    }
    
    /**
     * Obtém a chave de aplicação (app-key)
     * 
     * @return App key
     */
    public String getAppKey() {
        return appKey;
    }
    
    /**
     * Obtém a URL para obtenção do token
     * 
     * @return URL do token
     */
    public String getTokenUrl() {
        return tokenUrl;
    }
    
    /**
     * Obtém a URL base da API Pix
     * 
     * @return URL da API Pix
     */
    public String getPixUrl() {
        return pixUrl;
    }
    
    /**
     * Obtém o ambiente configurado
     * 
     * @return Nome do ambiente
     */
    public String getAmbiente() {
        return ambiente;
    }
}