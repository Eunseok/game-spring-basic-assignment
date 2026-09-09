package com.gamebasic.ranking.dto;

import java.util.List;

public record RankingResponse(
        String season,
        int totalRecords,
        int excludedCount,
        List<Entry> entries
) {
    public record Entry(
            int rank,
            String playerName,
            int clearTimeSeconds,
            int remainingHp,
            int bossTurns,
            int deckSize
    ) {}
}
