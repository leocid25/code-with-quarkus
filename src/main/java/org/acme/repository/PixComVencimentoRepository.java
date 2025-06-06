package org.acme.repository;

import java.time.LocalDate;
import java.util.List;

import org.acme.model.PixComVencimento;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PixComVencimentoRepository implements PanacheRepository<PixComVencimento> {

    /**
     * Busca uma cobrança Pix com vencimento pelo TxID
     * 
     * @param txid ID da transação
     * @return Objeto PixComVencimento se encontrado, null caso contrário
     */
    public PixComVencimento findByTxId(String txid) {
        return find("txid", txid).firstResult();
    }

    /**
     * Lista cobranças com vencimento por data
     * 
     * @param dataVencimento Data de vencimento a ser consultada
     * @return Lista de cobranças com a data de vencimento especificada
     */
    public List<PixComVencimento> listarPorDataVencimento(LocalDate dataVencimento) {
        return list("dataVencimento", dataVencimento);
    }

    /**
     * Lista cobranças vencidas até uma data específica
     * 
     * @param dataReferencia Data de referência para verificar vencimento
     * @return Lista de cobranças vencidas até a data informada
     */
    public List<PixComVencimento> listarVencidas(LocalDate dataReferencia) {
        return list("dataVencimento <= ?1 AND status = 'ATIVA'", dataReferencia);
    }

    /**
     * Lista cobranças próximas do vencimento (nos próximos X dias)
     * 
     * @param dataInicio Data inicial para filtro
     * @param dataFim    Data final para filtro
     * @return Lista de cobranças com vencimento entre as datas especificadas
     */
    public List<PixComVencimento> listarPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {

        // Usando a função list para buscar registros com data de vencimento entre as datas especificadas
        List<PixComVencimento> resultado = list("dataVencimento >= ?1 AND dataVencimento <= ?2", dataInicio, dataFim);
        return resultado;
    }

    /**
     * Conta o número de cobranças com vencimento para uma data específica
     * 
     * @param dataVencimento Data de vencimento
     * @return Quantidade de registros
     */
    public long contarPorDataVencimento(LocalDate dataVencimento) {
        return count("dataVencimento", dataVencimento);
    }

    /**
     * Lista cobranças de um devedor específico por CPF
     * 
     * @param cpf CPF do devedor
     * @return Lista de cobranças do devedor
     */
    public List<PixComVencimento> listarPorCpfDevedor(String cpf) {
        return list("cpf", cpf);
    }

    /**
     * Lista cobranças de um devedor específico por CNPJ
     * 
     * @param cnpj CNPJ do devedor
     * @return Lista de cobranças do devedor
     */
    public List<PixComVencimento> listarPorCnpjDevedor(String cnpj) {
        return list("cnpj", cnpj);
    }

    /**
     * Lista cobranças recentes por ordem de criação
     * 
     * @param limite Número máximo de registros
     * @return Lista de cobranças ordenadas pela data de criação (mais recentes
     *         primeiro)
     */
    public List<PixComVencimento> listarRecentes(int limite) {
        return find("ORDER BY criacao DESC")
                .page(0, limite)
                .list();
    }

    /**
     * Obtém o último ID registrado
     * 
     * @return O maior ID existente ou 0 se não houver registros
     */
    /**
     * Obtém o último ID registrado
     * 
     * @return O maior ID existente ou 0 se não houver registros
     */
    public Long obterUltimoId() {
        // Usando uma consulta direta que retorna o maior ID
        Long maxId = (Long) getEntityManager()
                .createQuery("SELECT MAX(p.id) FROM PixComVencimento p")
                .getSingleResult();

        return maxId != null ? maxId : 0L;
    }

    /**
     * Busca cobranças por status específico
     * 
     * @param status Status da cobrança (ex: "ATIVA", "CONCLUIDA",
     *               "REMOVIDA_PELO_USUARIO_RECEBEDOR")
     * @return Lista de cobranças com o status especificado
     */
    public List<PixComVencimento> listarPorStatus(String status) {
        return list("status", status);
    }

    /**
     * Filtra cobranças por data de vencimento e CPF do devedor
     * 
     * @param dataInicio Data inicial de vencimento
     * @param dataFim    Data final de vencimento
     * @param cpf        CPF do devedor
     * @return Lista de cobranças que atendem aos critérios
     */
    public List<PixComVencimento> filtrarPorDataECpf(LocalDate dataInicio, LocalDate dataFim, String cpf) {
        return list("dataVencimento BETWEEN ?1 AND ?2 AND cpf = ?3", dataInicio, dataFim, cpf);
    }

    /**
     * Filtra cobranças por data de vencimento e CNPJ do devedor
     * 
     * @param dataInicio Data inicial de vencimento
     * @param dataFim    Data final de vencimento
     * @param cnpj       CNPJ do devedor
     * @return Lista de cobranças que atendem aos critérios
     */
    public List<PixComVencimento> filtrarPorDataECnpj(LocalDate dataInicio, LocalDate dataFim, String cnpj) {
        return list("dataVencimento BETWEEN ?1 AND ?2 AND cnpj = ?3", dataInicio, dataFim, cnpj);
    }

    /**
     * Filtra cobranças por data de vencimento e status
     * 
     * @param dataInicio Data inicial de vencimento
     * @param dataFim    Data final de vencimento
     * @param status     Status da cobrança
     * @return Lista de cobranças que atendem aos critérios
     */
    public List<PixComVencimento> filtrarPorDataEStatus(LocalDate dataInicio, LocalDate dataFim, String status) {
        return list("dataVencimento BETWEEN ?1 AND ?2 AND status = ?3", dataInicio, dataFim, status);
    }

    /**
     * Filtra cobranças por data de vencimento, devedor (CPF ou CNPJ) e status
     * 
     * @param dataInicio    Data inicial de vencimento
     * @param dataFim       Data final de vencimento
     * @param identificador CPF ou CNPJ do devedor
     * @param isCpf         Flag indicando se o identificador é CPF (true) ou CNPJ
     *                      (false)
     * @param status        Status da cobrança
     * @return Lista de cobranças que atendem aos critérios
     */
    public List<PixComVencimento> filtrarCombinado(
            LocalDate dataInicio,
            LocalDate dataFim,
            String identificador,
            boolean isCpf,
            String status) {

        String campo = isCpf ? "cpf" : "cnpj";
        return list("dataVencimento BETWEEN ?1 AND ?2 AND " + campo + " = ?3 AND status = ?4",
                dataInicio, dataFim, identificador, status);
    }

    /**
     * Filtra cobranças com vencimento que ainda não foram pagas e já estão vencidas
     * 
     * @param dataReferencia Data de referência (geralmente data atual)
     * @return Lista de cobranças vencidas e não pagas
     */
    public List<PixComVencimento> listarVencidasNaoPagas(LocalDate dataReferencia) {
        return list("dataVencimento < ?1 AND status = 'ATIVA'", dataReferencia);
    }

    /**
     * Busca cobranças por múltiplos status
     * 
     * @param statusList Lista de status a serem incluídos na busca
     * @return Lista de cobranças que tenham um dos status especificados
     */
    public List<PixComVencimento> listarPorMultiplosStatus(List<String> statusList) {
        return list("status IN (?1)", statusList);
    }

    /**
     * Conta o número de cobranças por status e período de vencimento
     * 
     * @param dataInicio Data inicial de vencimento
     * @param dataFim    Data final de vencimento
     * @param status     Status para filtragem
     * @return Quantidade de registros
     */
    public long contarPorStatusEPeriodo(LocalDate dataInicio, LocalDate dataFim, String status) {
        return count("dataVencimento BETWEEN ?1 AND ?2 AND status = ?3", dataInicio, dataFim, status);
    }
}
