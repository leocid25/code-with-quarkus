package org.acme.repository;

import java.time.LocalDateTime;

import org.acme.model.TokenBB;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TokenRepository implements PanacheRepository<TokenBB> {
    
    /**
     * Busca o token mais recente armazenado no banco de dados
     * 
     * @return O token mais recente ou null se não houver tokens
     */
    public TokenBB findMostRecentToken() {
        return find("ORDER BY createdAt DESC")
                .firstResult();
    }
    
    /**
     * Busca um token válido (não expirado) no banco de dados
     * 
     * @param currentTime Data e hora atual para comparação
     * @return Token válido ou null se não houver token válido
     */
    public TokenBB findValidToken(LocalDateTime currentTime) {
        return find("expiresAt > ?1 ORDER BY expiresAt DESC", currentTime)
                .firstResult();
    }
    
    /**
     * Salva um novo token no banco de dados
     * 
     * @param token O token a ser salvo
     */
    public void saveToken(TokenBB token) {
        persist(token);
    }
    
    /**
     * Remove todos os tokens antigos, mantendo apenas o mais recente
     * 
     * @param mostRecentToken O token mais recente a ser mantido
     * @return Número de tokens removidos
     */
    public long deleteOldTokens(TokenBB mostRecentToken) {
        if (mostRecentToken == null) {
            return 0;
        }
        return delete("id != ?1", mostRecentToken.getId());
    }
    
    /**
     * Verifica se um token está expirado
     * 
     * @param token O token a ser verificado
     * @param currentTime Data e hora atual para comparação
     * @return true se o token estiver expirado, false caso contrário
     */
    public boolean isTokenExpired(TokenBB token, LocalDateTime currentTime) {
        if (token == null) {
            return true;
        }
        return token.getExpiresAt().isBefore(currentTime);
    }
}
