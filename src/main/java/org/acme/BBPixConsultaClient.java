// package org.acme;

// import java.net.URI;
// import java.net.http.HttpClient;
// import java.net.http.HttpRequest;
// import java.net.http.HttpResponse;
// import java.nio.charset.StandardCharsets;
// import java.time.Duration;
// import java.util.Base64;

// import io.quarkus.runtime.QuarkusApplication;
// import io.quarkus.runtime.annotations.QuarkusMain;
// import io.vertx.core.json.JsonObject;

// @QuarkusMain
// public class BBPixConsultaClient implements QuarkusApplication {

//     private static final String TOKEN_URL = "https://oauth.hm.bb.com.br/oauth/token";
//     private static final String PIX_COB_URL = "https://api.hm.bb.com.br/pix/v2/cob/";
//     private static final String CLIENT_ID = "eyJpZCI6ImJhNjAxYTgtYjdhYy00ZjMwLTgyMTYiLCJjb2RpZ29QdWJsaWNhZG9yIjowLCJjb2RpZ29Tb2Z0d2FyZSI6MTI3ODU5LCJzZXF1ZW5jaWFsSW5zdGFsYWNhbyI6MX0";
//     private static final String CLIENT_SECRET = "eyJpZCI6IjcxODI0YWQtODFlMy00NTBkLWJjZTAiLCJjb2RpZ29QdWJsaWNhZG9yIjowLCJjb2RpZ29Tb2Z0d2FyZSI6MTI3ODU5LCJzZXF1ZW5jaWFsSW5zdGFsYWNhbyI6MSwic2VxdWVuY2lhbENyZWRlbmNpYWwiOjEsImFtYmllbnRlIjoiaG9tb2xvZ2FjYW8iLCJpYXQiOjE3NDE0NTk1MDcyMjh9";
//     private static final String APP_KEY = "d212a6b98fbff07cdddbfd2f3dcc7878";

//     @Override
//     public int run(String... args) {
//         try {
//             // Verificar se foi fornecido um txid como argumento
//             if (args.length < 1) {
//                 System.err.println("É necessário fornecer o txid como argumento!");
//                 System.err.println("Uso: java -jar app.jar <txid>");
//                 return 1;
//             }
            
//             String txid = args[0];
//             System.out.println("Consultando cobrança com txid: " + txid);
            
//             // Obter token de acesso
//             String accessToken = getAccessToken();
//             System.out.println("Access Token obtido com sucesso!");
            
//             // Consultar cobrança PIX
//             String resultado = consultarCobrancaPix(accessToken, txid);
            
//             // Analisar resposta
//             analisarRespostaConsulta(resultado);
            
//             return 0;
//         } catch (Exception e) {
//             System.err.println("Erro ao processar operação PIX: " + e.getMessage());
//             return 1;
//         }
//     }

//     private String getAccessToken() throws Exception {
//         // Criar o cliente HTTP
//         HttpClient client = HttpClient.newBuilder()
//                 .version(HttpClient.Version.HTTP_1_1)
//                 .connectTimeout(Duration.ofSeconds(10))
//                 .build();

//         // Preparar o corpo da requisição
//         String requestBody = "grant_type=client_credentials";

//         // Criar a autorização Basic para o header
//         String auth = CLIENT_ID + ":" + CLIENT_SECRET;
//         String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

//         // Construir a requisição HTTP
//         HttpRequest request = HttpRequest.newBuilder()
//                 .uri(URI.create(TOKEN_URL))
//                 .header("Content-Type", "application/x-www-form-urlencoded")
//                 .header("Authorization", "Basic " + encodedAuth)
//                 .header("User-Agent", "Mozilla/5.0")
//                 .header("Accept", "application/json")
//                 .POST(HttpRequest.BodyPublishers.ofString(requestBody))
//                 .build();

//         // Enviar a requisição e obter a resposta
//         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

//         // Processar a resposta
//         if (response.statusCode() == 200) {
//             JsonObject jsonResponse = new JsonObject(response.body());
//             return jsonResponse.getString("access_token");
//         } else {
//             throw new RuntimeException("Falha na autenticação. Código: " + response.statusCode() + ", Resposta: " + response.body());
//         }
//     }

//     private String consultarCobrancaPix(String accessToken, String txid) throws Exception {
//         // Criar o cliente HTTP
//         HttpClient client = HttpClient.newBuilder()
//                 .version(HttpClient.Version.HTTP_1_1)
//                 .connectTimeout(Duration.ofSeconds(20))
//                 .build();

//         // Adicionar o parâmetro gw-dev-app-key como query parameter
//         String urlCompleta = PIX_COB_URL + txid + "?gw-dev-app-key=" + APP_KEY;
        
//         // Construir a requisição HTTP
//         HttpRequest request = HttpRequest.newBuilder()
//                 .uri(URI.create(urlCompleta))
//                 .header("Content-Type", "application/json")
//                 .header("Authorization", "Bearer " + accessToken)
//                 .header("Accept", "application/json")
//                 .GET()
//                 .build();

//         // Enviar a requisição e obter a resposta
//         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
//         System.out.println("Status da consulta: " + response.statusCode());
        
//         // Retornar a resposta completa
//         return response.body();
//     }
    
//     private void analisarRespostaConsulta(String jsonResposta) {
//         if (jsonResposta == null || jsonResposta.isEmpty()) {
//             System.out.println("Resposta vazia da API. Verifique se o txid existe.");
//             return;
//         }
        
//         try {
//             JsonObject resposta = new JsonObject(jsonResposta);
            
//             // Verificar se é uma resposta de erro
//             if (resposta.containsKey("type") && resposta.containsKey("title") && resposta.containsKey("status")) {
//                 System.out.println("Erro na consulta: " + resposta.getString("title") + " - " + resposta.getString("detail"));
//                 return;
//             }
            
//             // Status da cobrança
//             String status = resposta.getString("status", "N/A");
//             System.out.println("\n===== DADOS DA COBRANÇA PIX =====");
//             System.out.println("Status: " + status);
            
//             // Informações sobre o txid e valor
//             System.out.println("TxId: " + resposta.getString("txid", "N/A"));
            
//             if (resposta.containsKey("valor")) {
//                 JsonObject valor = resposta.getJsonObject("valor");
//                 System.out.println("Valor original: R$ " + valor.getString("original", "N/A"));
//             }
            
//             // Verificar se o PIX foi pago (verifica se há informações de pagamento)
//             if (resposta.containsKey("pix") && !resposta.getJsonArray("pix").isEmpty()) {
//                 System.out.println("\n===== PAGAMENTO CONFIRMADO =====");
//                 JsonObject pixInfo = resposta.getJsonArray("pix").getJsonObject(0);
                
//                 System.out.println("EndToEndId: " + pixInfo.getString("endToEndId", "N/A"));
//                 System.out.println("Valor pago: R$ " + pixInfo.getString("valor", "N/A"));
//                 System.out.println("Horário do pagamento: " + pixInfo.getString("horario", "N/A"));
                
//                 if (pixInfo.containsKey("infoPagador")) {
//                     System.out.println("Informação do pagador: " + pixInfo.getString("infoPagador", "N/A"));
//                 }
                
//                 // Verificar se houve devolução
//                 if (pixInfo.containsKey("devolucoes") && !pixInfo.getJsonArray("devolucoes").isEmpty()) {
//                     System.out.println("\n===== DEVOLUÇÃO DETECTADA =====");
//                     JsonObject devolucao = pixInfo.getJsonArray("devolucoes").getJsonObject(0);
                    
//                     System.out.println("Id da devolução: " + devolucao.getString("id", "N/A"));
//                     System.out.println("Valor devolvido: R$ " + devolucao.getString("valor", "N/A"));
//                     System.out.println("Status da devolução: " + devolucao.getString("status", "N/A"));
                    
//                     if (devolucao.containsKey("descricao")) {
//                         System.out.println("Descrição: " + devolucao.getString("descricao", "N/A"));
//                     }
//                 }
//             } else {
//                 if (status.equals("ATIVA")) {
//                     System.out.println("\n===== AGUARDANDO PAGAMENTO =====");
                    
//                     // Exibir QR Code para pagamento, se disponível
//                     if (resposta.containsKey("pixCopiaECola")) {
//                         System.out.println("\nPix Copia e Cola:");
//                         System.out.println(resposta.getString("pixCopiaECola"));
//                     }
                    
//                     // Exibir informação sobre expiração
//                     if (resposta.containsKey("calendario") && resposta.getJsonObject("calendario").containsKey("expiracao")) {
//                         int expiracao = resposta.getJsonObject("calendario").getInteger("expiracao");
//                         int expiracaoMinutos = expiracao / 60;
//                         System.out.println("Expira em: " + expiracaoMinutos + " minutos");
//                     }
//                 } else if (status.equals("CONCLUIDA")) {
//                     System.out.println("\n===== PAGAMENTO CONFIRMADO =====");
//                     System.out.println("(Detalhes do pagamento não disponíveis na resposta)");
//                 } else if (status.contains("REMOVIDA")) {
//                     System.out.println("\n===== COBRANÇA CANCELADA =====");
//                     System.out.println("A cobrança foi removida: " + status);
//                 }
//             }
            
//             // Exibir resposta completa para debug
//             System.out.println("\n===== RESPOSTA COMPLETA DA API =====");
//             System.out.println(jsonResposta);
            
//         } catch (Exception e) {
//             System.out.println("Erro ao processar resposta: " + e.getMessage());
//             System.out.println("Resposta bruta: " + jsonResposta);
//         }
//     }

//     public static void main(String[] args) {
//         io.quarkus.runtime.Quarkus.run(BBPixConsultaClient.class, args);
//     }
// }
