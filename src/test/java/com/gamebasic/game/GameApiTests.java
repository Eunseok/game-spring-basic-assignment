package com.gamebasic.game;


import com.gamebasic.game.dto.GameDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureRestTestClient
@Transactional
class GameApiTests {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    // ------------------------------------------------------------
    // 헬퍼 메서드
    // ------------------------------------------------------------

    private Map<String, Object> card(String cardType, int acquiredFloor) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("cardType", cardType);
        card.put("acquiredFloor", acquiredFloor);
        return card;
    }

    private String createRequestJson(String playerName, List<Map<String, Object>> deck) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("playerName", playerName);
        body.put("deck", deck);
        return objectMapper.writeValueAsString(body);
    }

    private Long createGameAndGetId() throws Exception {
        String requestJson = createRequestJson(
                "hero", List.of(card("STRIKE", 1), card("GUARD", 1))
        );

        GameDetailResponse response = restTestClient.post().uri("/games")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(GameDetailResponse.class)
                .returnResult()
                .getResponseBody();

        return response.getId();
    }

    private String renameRequestJson(String playerName) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("playerName", playerName);
        return objectMapper.writeValueAsString(map);
    }

    private String progressRequestJson(
            int currentHp, int currentFloor,
            String phase, String status,
            List<Map<String, Object>> deck
    ) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("currentHp", currentHp);
        body.put("currentFloor", currentFloor);
        body.put("phase", phase);
        body.put("status", status);
        body.put("deck", deck);
        return objectMapper.writeValueAsString(body);
    }

    // ------------------------------------------------------------
    // POST /games - 게임 생성
    // ------------------------------------------------------------

    @Test
    @DisplayName("게임을 생성하면 초기 상태와 덱이 저장되어 201로 반환된다")
    void createGame_success() throws Exception {
        // 초기 상태 currentHp=99, currentFloor=1, phase=REWARD, status=PLAYING
        String requestJson = createRequestJson(
                "hero",
                List.of(
                        card("STRIKE", 0),
                        card("GUARD", 0)
                )
        );

        restTestClient.post().uri("/games")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").exists()
                .jsonPath("$.playerName").isEqualTo("hero")
                .jsonPath("$.currentHp").isEqualTo(99)
                .jsonPath("$.currentFloor").isEqualTo(1)
                .jsonPath("$.phase").isEqualTo("REWARD")
                .jsonPath("$.status").isEqualTo("PLAYING")
                .jsonPath("$.deck.length()").isEqualTo(2)
                .jsonPath("$.deck[0].cardType").isEqualTo("STRIKE")
                .jsonPath("$.deck[0].acquiredFloor").isEqualTo(0)
                .jsonPath("$.deck[1].cardType").isEqualTo("GUARD")
                .jsonPath("$.deck[1].acquiredFloor").isEqualTo(0);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "a", "abcdefghijklmn"})
    @DisplayName("플레이어 이름이 2~12자를 벗어나면 400을 반환한다")
    void createGame_validationFail_invalidPlayerName(String playerName) throws Exception {
        String requestJson = createRequestJson(
                playerName,
                List.of(
                        card("STRIKE", 0),
                        card("GUARD", 0)
                )
        );

        restTestClient.post().uri("/games")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists();
    }

    @Test
    @DisplayName("덱 리스트가 비어있으면 400을 반환한다")
    void createGame_validationFail_emptyDeck() throws Exception {
        String requestJson = createRequestJson("hero", List.of());


        restTestClient.post().uri("/games")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11})
    @DisplayName("카드 획득 층이 0~10층을 벗어나면 400을 반환한다")
    void createGame_validationFail_invalidCardFloor(int acquiredFloor) throws Exception {
        String requestJson = createRequestJson(
                "hero",
                List.of(card("STRIKE", acquiredFloor))
        );

        restTestClient.post().uri("/games")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("카드 타입이 null이거나 비어있으면 400을 반환한다")
    void createGame_validationFail_invalidCardType(String cardType) throws Exception {
        String requestJson = createRequestJson(
                "hero",
                List.of(card(cardType, 0))
        );

        restTestClient.post().uri("/games")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists();
    }

    // ------------------------------------------------------------
    // GET /games  - 게임 목록 조회
    // ------------------------------------------------------------

    @Test
    @DisplayName("저장된 모든 게임을 ID 내림차순으로 반환한다")
    void getGames_success() throws Exception {
        // Given
        restTestClient.post().uri("/games")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequestJson("heroA", List.of(card("STRIKE", 0))))
                .exchange()
                .expectStatus().isCreated();

        restTestClient.post().uri("/games")
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequestJson("heroB", List.of(card("STRIKE", 0), card("GUARD", 0))))
                .exchange()
                .expectStatus().isCreated();


        // When & Then
        restTestClient.get().uri("/games")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].playerName").isEqualTo("heroB")
                .jsonPath("$[0].deckSize").isEqualTo(2)
                .jsonPath("$[1].playerName").isEqualTo("heroA")
                .jsonPath("$[1].deckSize").isEqualTo(1);
    }

    @Test
    @DisplayName("게임이 하나도 없으면 빈 목록을 반환한다")
    void getGames_empty() throws Exception {
        restTestClient.get().uri("/games")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isEmpty();
    }

    // ------------------------------------------------------------
    // GET /games/{id} - 게임 상세 조회
    // ------------------------------------------------------------

    @Test
    @DisplayName("생성한 게임을 id로 조회할 수 있다")
    void getGame_success() throws Exception {
        Long gameId = createGameAndGetId();

        restTestClient.get().uri("/games/{gameId}", gameId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(gameId)
                .jsonPath("$.playerName").isEqualTo("hero")
                .jsonPath("$.deck.length()").isEqualTo(2);
    }

    @Test
    @DisplayName("존재하지 않는 게임을 조회하면 404를 반환한다")
    void getGame_notFound() throws Exception {
        restTestClient.get().uri("/games/{gameId}", 999L)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.message")
                .value((String message) -> assertThat(message).contains("999"));
    }

    // ------------------------------------------------------------
    // PATCH /games/{id} - 플레이어 이름 변경
    // ------------------------------------------------------------

    @Test
    @DisplayName("이름을 변경하면 204를 반환하고 이후 조회 시 반영되어 있다")
    void renamePlayerName_success() throws Exception {
        Long gameId = createGameAndGetId();
        String requestJson;

        requestJson = """
                {
                    "playerName": "newName"
                }
                """;

        restTestClient.patch().uri("/games/{gameId}", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .exchange()
                .expectStatus().isNoContent();

        restTestClient.get().uri("/games/{gameId}", gameId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.playerName").isEqualTo("newName");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "a", "abcdefghijklmn"})
    @DisplayName("플레이어 이름이 2~12자를 벗어나면 400을 반환한다")
    void renamePlayerName_validationFail_invalidPlayerName(String playerName) throws Exception {
        Long gameId = createGameAndGetId();

        restTestClient.patch().uri("/games/{gameId}", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(renameRequestJson(playerName))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").exists();
    }

    @Test
    @DisplayName("존재하지 않는 게임의 이름을 변경하면 404를 반환한다")
    void renameGame_notFound() throws Exception {
        restTestClient.patch().uri("/games/{gameId}", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .body(renameRequestJson("newName"))
                .exchange()
                .expectStatus().isNotFound();
    }

    // ------------------------------------------------------------
    // DELETE /games/{id} - 게임 삭제
    // ------------------------------------------------------------

    @Test
    @DisplayName("게임을 삭제하면 204를 반환하고 이후 조회 시 404가 된다")
    void deleteGame_success() throws Exception {
        Long gameId = createGameAndGetId();

        restTestClient.delete().uri("/games/{gameId}", gameId)
                .exchange()
                .expectStatus().isNoContent();

        restTestClient.get().uri("/games/{gameId}", gameId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("존재하지 않는 게임을 삭제하면 404를 반환한다")
    void deleteGame_notFound() throws Exception {
        restTestClient.delete().uri("/games/{gameId}", 999L)
                .exchange()
                .expectStatus().isNotFound();
    }

    // ------------------------------------------------------------
    // PUT /games/{id}/progress - 진행과 전체 덱 저장
    // ------------------------------------------------------------
    @Test
    @DisplayName("HP, 층, 단계, 상태와 전체 덱을 저장한다. deck은 추가할 카드가 아니라 저장할 덱 전체다.")
    void updateProgress_success() throws Exception {
        Long gameId = createGameAndGetId();

        String requestJson = progressRequestJson(
                50, 3, "BATTLE", "PLAYING", List.of(card("HEAVY_BLOW", 3), card("STRIKE", 1))
        );

        restTestClient.put().uri("/games/{gameId}/progress", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(gameId)
                .jsonPath("$.currentHp").isEqualTo(50)
                .jsonPath("$.currentFloor").isEqualTo(3)
                .jsonPath("$.phase").isEqualTo("BATTLE")
                .jsonPath("$.status").isEqualTo("PLAYING")
                .jsonPath("$.deck.length()").isEqualTo(2)
                .jsonPath("$.deck[0].cardType").isEqualTo("HEAVY_BLOW")
                .jsonPath("$.deck[1].cardType").isEqualTo("STRIKE");

        // 재조회해도 갱신된 내용이 유지되어야 한다
        restTestClient.get().uri("/games/{gameId}", gameId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.currentHp").isEqualTo(50)
                .jsonPath("$.deck.length()").isEqualTo(2);
    }

    @Test
    @DisplayName("존재하지 않는 게임의 진행 상황을 업데이트하면 404를 반환한다")
    void updateProgress_notFound() throws Exception {
        String requestJson = progressRequestJson(
                50, 3, "BATTLE", "PLAYING", List.of(card("STRIKE", 1))
        );

        restTestClient.put().uri("/games/{gameId}/progress", 999_999L)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestJson)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("이미 종료된 게임의 진행 상황을 업데이트하려 하면 409를 반환한다")
    void updateProgress_whenGameFinished_conflict() throws Exception {
        Long gameId = createGameAndGetId();

        // 먼저 게임을 CLEARED 상태로 종료시킨다
        restTestClient.put().uri("/games/{gameId}/progress", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(progressRequestJson(
                        99, 10, "FINISHED", "CLEARED", List.of(card("STRIKE", 1))
                ))
                .exchange()
                .expectStatus().isOk();

        // 종료된 게임에 다시 진행 상황을 저장하려고 하면 실패해야 한다
        restTestClient.put().uri("/games/{gameId}/progress", gameId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(progressRequestJson(
                        80, 5, "BATTLE", "PLAYING", List.of(card("STRIKE", 1))
                ))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409);
    }

    // TODO ProgressRequest 제약 검증
}