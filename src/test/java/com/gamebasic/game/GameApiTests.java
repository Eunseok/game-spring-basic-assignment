package com.gamebasic.game;

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
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("게임을 생성하면 초기 상태와 덱이 저장되어 201로 반환된다")
    void createGame_success() throws Exception {
        // 초기 상태 currentHp=99, currentFloor=1, phase=REWARD, status=PLAYING
        String requestJson = """
                {
                  "playerName": "hero",
                  "deck": [
                    {
                      "cardType": "STRIKE",
                      "acquiredFloor": 0
                    },
                    {
                      "cardType": "GUARD",
                      "acquiredFloor": 0
                    }
                  ]
                }
                """;

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
        String playerNameJson = objectMapper.writeValueAsString(playerName);

        String requestJson = """
                {
                  "playerName": %s,
                  "deck": [
                    {
                      "cardType": "STRIKE",
                      "acquiredFloor": 0
                    },
                    {
                      "cardType": "GUARD",
                      "acquiredFloor": 0
                    }
                  ]
                }
                """.formatted(playerNameJson);

        mockMvc.perform(
                        post("/games")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }
}
