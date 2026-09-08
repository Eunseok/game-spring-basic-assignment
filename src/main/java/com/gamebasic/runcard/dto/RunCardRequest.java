package com.gamebasic.runcard.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class RunCardRequest {
    @NotBlank(message = "카드 타입은 필수 입력값입니다.")
    private String cardType;
    @Min(value = 0, message = "획득한 층은 0 이상이어야 합니다.")
    @Max(value = 10, message = "획득한 층은 10 이하여야 합니다.")
    private Integer acquiredFloor;
}
