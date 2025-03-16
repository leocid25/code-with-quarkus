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
// import io.vertx.core.json.JsonArray;
// import io.vertx.core.json.JsonObject;

// @QuarkusMain
// public class BBPixCobrancaClient implements QuarkusApplication {

//     private static final String TOKEN_URL = "https://oauth.hm.bb.com.br/oauth/token";
//     private static final String PIX_COB_URL = "https://api.hm.bb.com.br/pix/v2/cob/";
//     private static final String CLIENT_ID = "eyJpZCI6ImJhNjAxYTgtYjdhYy00ZjMwLTgyMTYiLCJjb2RpZ29QdWJsaWNhZG9yIjowLCJjb2RpZ29Tb2Z0d2FyZSI6MTI3ODU5LCJzZXF1ZW5jaWFsSW5zdGFsYWNhbyI6MX0";
//     private static final String CLIENT_SECRET = "eyJpZCI6IjcxODI0YWQtODFlMy00NTBkLWJjZTAiLCJjb2RpZ29QdWJsaWNhZG9yIjowLCJjb2RpZ29Tb2Z0d2FyZSI6MTI3ODU5LCJzZXF1ZW5jaWFsSW5zdGFsYWNhbyI6MSwic2VxdWVuY2lhbENyZWRlbmNpYWwiOjEsImFtYmllbnRlIjoiaG9tb2xvZ2FjYW8iLCJpYXQiOjE3NDE0NTk1MDcyMjh9";

//     @Override
//     public int run(String... args) {
//         try {
//             // Obter token de acesso
//             String accessToken = getAccessToken();
//             System.out.println("Access Token obtido com sucesso!");
            
//             // Gerar txid único (entre 26-35 caracteres) sem caracteres especiais
//             // Usando apenas letras e números conforme exigido pelo Banco do Brasil
//             String txid = "Teste" + System.currentTimeMillis() + "X";
//             // Garantir que tenha entre 26 e 35 caracteres
//             if (txid.length() < 26) {
//                 // Preencher com zeros à direita se for muito curto
//                 txid = txid + "0".repeat(26 - txid.length());
//             } else if (txid.length() > 35) {
//                 // Cortar se for muito longo
//                 txid = txid.substring(0, 35);
//             }
//             System.out.println("TxID gerado: " + txid);
            
//             // Criar cobrança PIX
//             String resultado = criarCobrancaPix(accessToken, txid);
//             System.out.println("Resposta da criação da cobrança PIX:");
//             System.out.println(resultado);
            
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

//     private String criarCobrancaPix(String accessToken, String txid) throws Exception {
//         // Criar o cliente HTTP
//         HttpClient client = HttpClient.newBuilder()
//                 .version(HttpClient.Version.HTTP_1_1)
//                 .connectTimeout(Duration.ofSeconds(20))
//                 .build();

//         // Criar o JSON de cobrança PIX
//         JsonObject cobrancaJson = criarJsonCobranca();
        
//         // Adicionar o parâmetro gw-dev-app-key como query parameter
//         String urlCompleta = PIX_COB_URL + txid + "?gw-dev-app-key=d212a6b98fbff07cdddbfd2f3dcc7878";
        
//         // Construir a requisição HTTP
//         HttpRequest request = HttpRequest.newBuilder()
//                 .uri(URI.create(urlCompleta))
//                 .header("Content-Type", "application/json")
//                 .header("Authorization", "Bearer " + accessToken)
//                 .header("Accept", "application/json")
//                 .PUT(HttpRequest.BodyPublishers.ofString(cobrancaJson.encode()))
//                 .build();

//         // Enviar a requisição e obter a resposta
//         HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

//         // Retornar a resposta completa
//         return response.body();
//     }

//     private JsonObject criarJsonCobranca() {
//         // Criar o objeto JSON completo de acordo com a documentação
//         JsonObject cobranca = new JsonObject();
        
//         // Adicionar objeto calendario
//         JsonObject calendario = new JsonObject()
//                 .put("expiracao", 3600);
//         cobranca.put("calendario", calendario);
        
//         // Adicionar objeto devedor
//         JsonObject devedor = new JsonObject()
//                 .put("cnpj", "12345678000195")
//                 .put("nome", "Empresa de Serviços SA");
//         cobranca.put("devedor", devedor);
        
//         // Adicionar objeto valor
//         JsonObject valor = new JsonObject()
//                 .put("original", "37.00");
//         cobranca.put("valor", valor);
        
//         // Adicionar chave PIX (pode ser CPF, CNPJ, telefone, email ou EVP)
//         cobranca.put("chave", "9e881f18-cc66-4fc7-8f2c-a795dbb2bfc1");
        
//         // Adicionar solicitação ao pagador
//         cobranca.put("solicitacaoPagador", "Serviço realizado.");
        
//         // Adicionar informações adicionais
//         JsonArray infoAdicionais = new JsonArray();
        
//         JsonObject info1 = new JsonObject()
//                 .put("nome", "Campo 1")
//                 .put("valor", "Informação Adicional1 do PSP-Recebedor");
        
//         JsonObject info2 = new JsonObject()
//                 .put("nome", "Campo 2")
//                 .put("valor", "Informação Adicional2 do PSP-Recebedor");
        
//         infoAdicionais.add(info1).add(info2);
//         cobranca.put("infoAdicionais", infoAdicionais);
        
//         return cobranca;
//     }

//     public static void main(String[] args) {
//         io.quarkus.runtime.Quarkus.run(BBPixCobrancaClient.class, args);
//     }
// }