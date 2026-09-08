package com.gamebasic.game.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RenameRequest {

    @Size(min = 2, max = 12, message = "플레이어 이름은 2자 이상 12자 이하여야 합니다.")
    private String playerName;
}
