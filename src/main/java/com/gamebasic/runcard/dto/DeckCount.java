package com.gamebasic.runcard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeckCount {
    Long gameId;
    Long deckSize;
}
