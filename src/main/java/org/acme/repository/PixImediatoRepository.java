package org.acme.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.acme.model.PixImediato;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PixImediatoRepository implements PanacheRepository<PixImediato> {

    /**
     * Busca um PixImediato pelo TxID
     * 
     * @param txid ID da transação
     * @return PixImediato correspondente ou null se não encontrado
     */
    public PixImediato findByTxId(String txid) {
        return find("txid", txid).firstResult();
    }

    /**
     * Obtém o último ID da tabela de Pix (entidade abstrata que é pai de
     * PixImediato e PixComVencimento)
     * 
     * @return O último ID ou 0 se não houver registros
     */
    public Long obterUltimoId() {
        // Buscar o maior ID da tabela Pix, retornando 0 se for NULL (tabela vazia)
        Long maxId = getEntityManager()
                .createQuery("SELECT MAX(p.id) FROM Pix p", Long.class)
                .getSingleResult();

        return maxId != null ? maxId : 0L;
    }

    /**
     * Lista cobranças imediatas que expiram entre as datas especificadas
     * 
     * @param dataInicio Data inicial para filtro de expiração
     * @param dataFim    Data final para filtro de expiração
     * @return Lista de cobranças que expiram entre as datas especificadas
     */
    /**
     * Lista cobranças imediatas que expiram entre as datas especificadas
     */
    public List<PixImediato> listarPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        // Busca todas as cobranças ativas
        List<PixImediato> todasCobrancas = list("status = 'ATIVA'");

        return todasCobrancas.stream()
                .filter(pix -> {
                    // Calcula quando a cobrança expira
                    LocalDateTime dataExpiracao = pix.getCriacao().plusSeconds(pix.getExpiracao());
                    LocalDate dataExpiracaoDate = dataExpiracao.toLocalDate();

                    // Verifica se a data de expiração está no intervalo
                    return !dataExpiracaoDate.isBefore(dataInicio) &&
                            !dataExpiracaoDate.isAfter(dataFim);
                })
                .collect(Collectors.toList());
    }
}
