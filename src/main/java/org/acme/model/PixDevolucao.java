package org.acme.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;

/**
 * Classe que representa uma devolução de uma transação Pix
 */
@Entity
public class PixDevolucao extends DefaultEntity{

    private String rtrId;
    private BigDecimal valor;
    private String natureza; // ORIGINAL, RETIRADA, MED_OPERACIONAL, MED_FRAUDE
    private String descricao;
    private LocalDateTime horarioSolicitacao;
    private LocalDateTime horarioLiquidacao;
    private String status; // EM_PROCESSAMENTO, DEVOLVIDO, NAO_REALIZADO
    private String motivo;
    
    /**
     * Construtor padrão
     */
    public PixDevolucao() {
        this.natureza = "ORIGINAL";
        this.status = "EM_PROCESSAMENTO";
        this.horarioSolicitacao = LocalDateTime.now();
    }
    
    /**
     * Construtor com os campos obrigatórios
     *
     * @param valor Valor da devolução
     */
    public PixDevolucao(BigDecimal valor) {
        this();
        this.valor = valor;
    }
    
    /**
     * Construtor com os campos obrigatórios e natureza personalizada
     *
     * @param valor Valor da devolução
     * @param natureza Natureza da devolução (ORIGINAL, RETIRADA, MED_OPERACIONAL, MED_FRAUDE)
     */
    public PixDevolucao(BigDecimal valor, String natureza) {
        this.natureza = natureza;
    }
    
    /**
     * Construtor completo
     *
     * @param valor Valor da devolução
     * @param natureza Natureza da devolução
     * @param descricao Descrição da devolução
     */
    public PixDevolucao(BigDecimal valor, String natureza, String descricao) {
        this(valor, natureza);
        this.descricao = descricao;
    }
    
    /**
     * Marca a devolução como concluída
     *
     * @param rtrId ID de retorno
     */
    public void concluirDevolucao(String rtrId) {
        this.rtrId = rtrId;
        this.status = "DEVOLVIDO";
        this.horarioLiquidacao = LocalDateTime.now();
    }
    
    /**
     * Marca a devolução como não realizada
     *
     * @param motivo Motivo da falha na devolução
     */
    public void falharDevolucao(String motivo) {
        this.status = "NAO_REALIZADO";
        this.motivo = motivo;
    }
    
    /**
     * Verifica se a devolução está em processamento
     * 
     * @return true se estiver em processamento, false caso contrário
     */
    public boolean isEmProcessamento() {
        return "EM_PROCESSAMENTO".equals(this.status);
    }
    
    /**
     * Verifica se a devolução foi concluída com sucesso
     * 
     * @return true se foi devolvido, false caso contrário
     */
    public boolean isDevolvido() {
        return "DEVOLVIDO".equals(this.status);
    }
    
    /**
     * Verifica se a devolução falhou
     * 
     * @return true se não foi realizada, false caso contrário
     */
    public boolean isFalhou() {
        return "NAO_REALIZADO".equals(this.status);
    }
    
    // Getters e setters
    
    public String getRtrId() {
        return rtrId;
    }
    
    public void setRtrId(String rtrId) {
        this.rtrId = rtrId;
    }
    
    public BigDecimal getValor() {
        return valor;
    }
    
    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
    
    public String getNatureza() {
        return natureza;
    }
    
    public void setNatureza(String natureza) {
        this.natureza = natureza;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
    
    public LocalDateTime getHorarioSolicitacao() {
        return horarioSolicitacao;
    }
    
    public void setHorarioSolicitacao(LocalDateTime horarioSolicitacao) {
        this.horarioSolicitacao = horarioSolicitacao;
    }
    
    public LocalDateTime getHorarioLiquidacao() {
        return horarioLiquidacao;
    }
    
    public void setHorarioLiquidacao(LocalDateTime horarioLiquidacao) {
        this.horarioLiquidacao = horarioLiquidacao;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMotivo() {
        return motivo;
    }
    
    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
    
    @Override
    public String toString() {
        return "Devolução #" + this.getId() + " - R$ " + valor + " - Status: " + status;
    }
}