package com.gamebasic.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class GameSummaryResponse {
    private Long id;
    private String playerName;
    private int currentFloor;
    private int currentHp;
    private String phase;
    private String status;
    private int deckSize;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;
}
