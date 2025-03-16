package org.acme.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Entidade que representa um token de acesso da API Pix do Banco do Brasil
 */
@Entity
@Table(name = "token_bb")
public class TokenBB extends DefaultEntity{
    
    @Column(name = "access_token", length = 2000, nullable = false)
    private String accessToken;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    /**
     * Construtor padrão
     */
    public TokenBB() {
    }
    
    /**
     * Construtor com parâmetros
     * 
     * @param accessToken Token de acesso
     * @param createdAt Data/hora de criação
     * @param expiresAt Data/hora de expiração
     */
    public TokenBB(String accessToken, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.accessToken = accessToken;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
    
    /**
     * Verifica se o token ainda é válido
     * 
     * @return true se o token ainda for válido, false caso contrário
     */
    public boolean isValid() {
        return LocalDateTime.now().isBefore(expiresAt);
    }
    
    // Getters e Setters
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}