package com.gamebasic.runcard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CardResponse {
    private Long id;
    private String cardType;
    private int acquiredFloor;
}
