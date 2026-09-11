package com.gamebasic.game;

import com.gamebasic.game.dto.GameDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class GameApiTests {

    @Autowired
    private MockMvc mockMvc;

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

        MvcResult result = mockMvc.perform(post("/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andReturn();

        GameDetailResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                GameDetailResponse.class
        );

        return response.getId();
    }

    private String renameRequestJson(String playerName) throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("playerName", playerName); 
        return objectMapper.writeValueAsString(map);
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

        mockMvc.perform(
                        post("/games")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.playerName").value("hero"))
                .andExpect(jsonPath("$.currentHp").value(99))
                .andExpect(jsonPath("$.currentFloor").value(1))
                .andExpect(jsonPath("$.phase").value("REWARD"))
                .andExpect(jsonPath("$.status").value("PLAYING"))
                .andExpect(jsonPath("$.deck", hasSize(2)))
                .andExpect(jsonPath("$.deck[0].cardType").value("STRIKE"))
                .andExpect(jsonPath("$.deck[0].acquiredFloor").value(0))
                .andExpect(jsonPath("$.deck[1].cardType").value("GUARD"))
                .andExpect(jsonPath("$.deck[1].acquiredFloor").value(0));
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

        mockMvc.perform(
                        post("/games")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("덱 리스트가 비어있으면 400을 반환한다")
    void createGame_validationFail_emptyDeck() throws Exception {
        String requestJson = createRequestJson("hero", List.of());

        mockMvc.perform(
                        post("/games")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 11})
    @DisplayName("카드 획득 층이 0~10층을 벗어나면 400을 반환한다")
    void createGame_validationFail_invalidCardFloor(int acquiredFloor) throws Exception {
        String requestJson = createRequestJson(
                "hero",
                List.of(card("STRIKE", acquiredFloor))
        );

        mockMvc.perform(
                        post("/games")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
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

        mockMvc.perform(
                        post("/games")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // ------------------------------------------------------------
    // GET /games  - 게임 목록 조회
    // ------------------------------------------------------------

    @Test
    @DisplayName("저장된 모든 게임을 ID 내림차순으로 반환한다")
    void getGames_success() throws Exception {
        // Given
        mockMvc.perform(
                        post("/games")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestJson("heroA", List.of(card("STRIKE", 0))))
                )
                .andExpect(status().isCreated());
        mockMvc.perform(
                        post("/games")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createRequestJson("heroB", List.of(card("STRIKE", 0), card("GUARD", 0))))
                )
                .andExpect(status().isCreated());

        // When & Then
        mockMvc.perform(get("/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].playerName").value("heroB"))
                .andExpect(jsonPath("$[0].deckSize").value(2))
                .andExpect(jsonPath("$[1].playerName").value("heroA"))
                .andExpect(jsonPath("$[1].deckSize").value(1));
    }

    @Test
    @DisplayName("게임이 하나도 없으면 빈 목록을 반환한다")
    void getGames_empty() throws Exception {
        mockMvc.perform(get("/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ------------------------------------------------------------
    // GET /games/{id} - 게임 상세 조회
    // ------------------------------------------------------------

    @Test
    @DisplayName("생성한 게임을 id로 조회할 수 있다")
    void getGame_success() throws Exception {
        Long gameId = createGameAndGetId();

        mockMvc.perform(get("/games/{gameId}", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(gameId))
                .andExpect(jsonPath("$.playerName").value("hero"))
                .andExpect(jsonPath("$.deck", hasSize(2)));
    }

    @Test
    @DisplayName("존재하지 않는 게임을 조회하면 404를 반환한다")
    void getGame_notFound() throws Exception {
        mockMvc.perform(get("/games/{gameId}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("999")));
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
        mockMvc.perform(
                        patch("/games/{gameId}", gameId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/games/{gameId}", gameId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerName").value("newName"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "a", "abcdefghijklmn"})
    @DisplayName("플레이어 이름이 2~12자를 벗어나면 400을 반환한다")
    void renamePlayerName_validationFail_invalidPlayerName(String playerName) throws Exception {
        Long gameId = createGameAndGetId();

        mockMvc.perform(
                        patch("/games/{gameId}", gameId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(renameRequestJson(playerName))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("존재하지 않는 게임의 이름을 변경하면 404를 반환한다")
    void renameGame_notFound() throws Exception {
        mockMvc.perform(patch("/games/{gameId}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(renameRequestJson("newName")))
                .andExpect(status().isNotFound());
    }
}
