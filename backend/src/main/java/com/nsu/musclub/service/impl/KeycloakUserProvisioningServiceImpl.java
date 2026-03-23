package com.nsu.musclub.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nsu.musclub.dto.user.UserCreateDto;
import com.nsu.musclub.exception.ResourceAlreadyExistsException;
import com.nsu.musclub.service.KeycloakUserProvisioningService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class KeycloakUserProvisioningServiceImpl implements KeycloakUserProvisioningService {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.base-url}")
    private String keycloakBaseUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;

    @Value("${keycloak.admin.client-id}")
    private String keycloakAdminClientId;

    @Value("${keycloak.admin.username}")
    private String keycloakAdminUsername;

    @Value("${keycloak.admin.password}")
    private String keycloakAdminPassword;

    public KeycloakUserProvisioningServiceImpl(ObjectMapper objectMapper) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
    }

    @Override
    public String createUserAndAssignRole(UserCreateDto dto) {
        String accessToken = getAdminAccessToken();

        String createUserUrl = keycloakBaseUrl + "/admin/realms/" + keycloakRealm + "/users";

        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("username", dto.getUsername());
            payload.put("email", dto.getEmail());
            payload.put("enabled", true);
            // Чтобы пользователь мог логиниться сразу через password grant.
            // Если в realm требуется verify email, то с emailVerified=false выдача токенов будет блокироваться.
            payload.put("emailVerified", true);
            // На всякий случай убираем required actions, если Keycloak/realm их добавляет по умолчанию.
            payload.putArray("requiredActions");

            ArrayNode credentials = payload.putArray("credentials");
            ObjectNode passwordCred = credentials.addObject();
            passwordCred.put("type", "password");
            passwordCred.put("value", dto.getPassword());
            passwordCred.put("temporary", false);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(createUserUrl))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 409) {
                throw new ResourceAlreadyExistsException("Пользователь", "email/username", dto.getEmail());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Keycloak user create failed: status=" + response.statusCode() + ", body=" + response.body());
            }

            String userId = response.headers().firstValue("Location")
                    .flatMap(loc -> {
                        int idx = loc.lastIndexOf('/');
                        if (idx >= 0 && idx + 1 < loc.length()) {
                            return java.util.Optional.of(loc.substring(idx + 1));
                        }
                        return java.util.Optional.empty();
                    })
                    .orElse(null);

            if (userId == null || userId.isBlank()) {
                // На практике Location обычно возвращают, но если нет — пробуем найти по email.
                userId = findUserIdByEmail(accessToken, dto.getEmail());
            }

            assignRealmRole(accessToken, userId, dto.getRole());
            return userId;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Keycloak user create failed", e);
        }
    }

    @Override
    public void deleteUserQuietly(String keycloakUserId) {
        if (keycloakUserId == null || keycloakUserId.isBlank()) return;

        String accessToken = getAdminAccessToken();
        String deleteUrl = keycloakBaseUrl + "/admin/realms/" + keycloakRealm + "/users/" + keycloakUserId;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deleteUrl))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .DELETE()
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // best effort rollback
        }
    }

    private String getAdminAccessToken() {
        String tokenUrl = keycloakBaseUrl + "/realms/master/protocol/openid-connect/token";

        String body = "grant_type=password" +
                "&client_id=" + urlEncode(keycloakAdminClientId) +
                "&username=" + urlEncode(keycloakAdminUsername) +
                "&password=" + urlEncode(keycloakAdminPassword);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Keycloak admin token failed: status=" + response.statusCode() + ", body=" + response.body());
            }

            JsonNode node = objectMapper.readTree(response.body());
            return node.get("access_token").asText();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Keycloak admin token failed", e);
        }
    }

    private void assignRealmRole(String accessToken, String keycloakUserId, String roleName) {
        if (roleName == null || roleName.isBlank() || keycloakUserId == null || keycloakUserId.isBlank()) return;

        // Для текущего UI достаточно MEMBER/ORGANIZER
        try {
            String roleGetUrl = keycloakBaseUrl + "/admin/realms/" + keycloakRealm + "/roles/" + urlEncode(roleName);
            HttpRequest roleGetRequest = HttpRequest.newBuilder()
                    .uri(URI.create(roleGetUrl))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .GET()
                    .build();

            HttpResponse<String> roleGetResponse = httpClient.send(roleGetRequest, HttpResponse.BodyHandlers.ofString());
            if (roleGetResponse.statusCode() == 404) {
                // роль может отсутствовать — создадим
                createRoleIfMissing(accessToken, roleName);

                // После создания повторно запросим роль
                roleGetResponse = httpClient.send(roleGetRequest, HttpResponse.BodyHandlers.ofString());
            }

            if (roleGetResponse.statusCode() < 200 || roleGetResponse.statusCode() >= 300) {
                throw new RuntimeException(
                        "Keycloak role lookup failed: status=" + roleGetResponse.statusCode() + ", body=" + roleGetResponse.body()
                );
            }

            JsonNode roleNode = objectMapper.readTree(roleGetResponse.body());
            String roleId = roleNode.has("id") ? roleNode.get("id").asText() : null;
            String resolvedRoleName = roleNode.has("name") ? roleNode.get("name").asText() : roleName;

            if (roleId == null || roleId.isBlank()) return;

            ArrayNode rolesArray = objectMapper.createArrayNode();
            ObjectNode mappingRole = rolesArray.addObject();
            mappingRole.put("id", roleId);
            mappingRole.put("name", resolvedRoleName);

            String assignUrl = keycloakBaseUrl + "/admin/realms/" + keycloakRealm + "/users/" + keycloakUserId + "/role-mappings/realm";
            HttpRequest assignRequest = HttpRequest.newBuilder()
                    .uri(URI.create(assignUrl))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(rolesArray.toString()))
                    .build();

            HttpResponse<String> assignResponse = httpClient.send(assignRequest, HttpResponse.BodyHandlers.ofString());
            if (assignResponse.statusCode() < 200 || assignResponse.statusCode() >= 300) {
                throw new RuntimeException("Keycloak assign role failed: status=" + assignResponse.statusCode() + ", body=" + assignResponse.body());
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Keycloak assign role failed", e);
        }
    }

    private void createRoleIfMissing(String accessToken, String roleName) throws IOException, InterruptedException {
        String createRoleUrl = keycloakBaseUrl + "/admin/realms/" + keycloakRealm + "/roles";

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("name", roleName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(createRoleUrl))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            // если роль уже создана конкурентно — игнорируем
            if (response.statusCode() != 409) {
                throw new RuntimeException("Keycloak create role failed: status=" + response.statusCode() + ", body=" + response.body());
            }
        }
    }

    private String findUserIdByEmail(String accessToken, String email) throws IOException, InterruptedException {
        String searchUrl = keycloakBaseUrl + "/admin/realms/" + keycloakRealm + "/users?email=" + urlEncode(email);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(searchUrl))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Keycloak find user by email failed: status=" + response.statusCode());
        }

        JsonNode arr = objectMapper.readTree(response.body());
        if (arr.isArray() && arr.size() > 0) {
            JsonNode first = arr.get(0);
            if (first.has("id")) return first.get("id").asText();
        }
        throw new RuntimeException("Keycloak created user but cannot find it by email");
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

