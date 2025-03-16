// package org.acme;

// import io.quarkus.runtime.QuarkusApplication;
// import io.quarkus.runtime.annotations.QuarkusMain;

// @QuarkusMain
// public class BBOAuthClient implements QuarkusApplication {

    // private static final String TOKEN_URL = "https://oauth.hm.bb.com.br/oauth/token";
    // private static final String CLIENT_ID = "eyJpZCI6ImJhNjAxYTgtYjdhYy00ZjMwLTgyMTYiLCJjb2RpZ29QdWJsaWNhZG9yIjowLCJjb2RpZ29Tb2Z0d2FyZSI6MTI3ODU5LCJzZXF1ZW5jaWFsSW5zdGFsYWNhbyI6MX0"; // Substitua pelo seu client_id
    // private static final String CLIENT_SECRET = "eyJpZCI6IjcxODI0YWQtODFlMy00NTBkLWJjZTAiLCJjb2RpZ29QdWJsaWNhZG9yIjowLCJjb2RpZ29Tb2Z0d2FyZSI6MTI3ODU5LCJzZXF1ZW5jaWFsSW5zdGFsYWNhbyI6MSwic2VxdWVuY2lhbENyZWRlbmNpYWwiOjEsImFtYmllbnRlIjoiaG9tb2xvZ2FjYW8iLCJpYXQiOjE3NDE0NTk1MDcyMjh9"; // Substitua pelo seu client_secret

    //  @Override
    //  public int run(String... args) {
    //     try {
    //         String accessToken = getAccessToken();
    //         System.out.println("Access Token:");
    //         System.out.println(accessToken);
    //         return 0;
    //     } catch (Exception e) {
    //         System.err.println("Erro ao obter access token: " + e.getMessage());
    //         return 1;
    //     }
    // }

    // private String getAccessToken() throws Exception {
    //     // Criar o cliente HTTP
    //     HttpClient client = HttpClient.newBuilder()
    //             .version(HttpClient.Version.HTTP_1_1)
    //             .build();

    //     // Preparar o corpo da requisição
    //     String requestBody = "grant_type=client_credentials";

    //     // Criar a autorização Basic para o header
    //     String auth = CLIENT_ID + ":" + CLIENT_SECRET;
    //     String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

    //     // Construir a requisição HTTP
    //     HttpRequest request = HttpRequest.newBuilder()
    //             .uri(URI.create(TOKEN_URL))
    //             .header("Content-Type", "application/x-www-form-urlencoded")
    //             .header("Authorization", "Basic " + encodedAuth)
    //             .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
    //             .header("Accept", "application/json")
    //             .POST(HttpRequest.BodyPublishers.ofString(requestBody))
    //             .build();

    //     // Enviar a requisição e obter a resposta
    //     HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    //     // Processar a resposta
    //     if (response.statusCode() == 200) {
    //         JsonObject jsonResponse = new JsonObject(response.body());
            
    //         // Imprimir resposta completa para debug
    //         System.out.println("Resposta completa do servidor:");
    //         System.out.println(response.body());
            
    //         return jsonResponse.getString("access_token");
    //     } else {
    //         throw new RuntimeException("Falha na autenticação. Código: " + response.statusCode() + ", Resposta: " + response.body());
    //     }
    // }

    // public static void main(String[] args) {
    //     io.quarkus.runtime.Quarkus.run(BBOAuthClient.class, args);
    // }
// }