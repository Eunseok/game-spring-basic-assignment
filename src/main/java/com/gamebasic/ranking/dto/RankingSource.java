package com.gamebasic.ranking.dto;

import java.time.Instant;
import java.util.List;

public record RankingSource(
        Meta meta,
        List<RankingRecord> records
) {

    public record Meta(
            Season season,
            Instant generatedAt,
            int schemaVersion,
            int totalRecords
    ) {}

    public record Season(
            String id,
            String name,
            Instant startsAt,
            Instant endsAt
    ) {}

    public record RankingRecord(
            long id,
            Instant submittedAt,
            ClientInfo client,
            Player player,
            Run run,
            BossFight bossFight,
            Deck deck
    ) {}

    public record ClientInfo(
            String version,
            String platform,
            String locale
    ) {}

    public record Player(
            String id,
            String name,
            String region,
            List<String> tags
    ) {}

    public record Run(
            String seed,
            String status,
            int clearedFloor,
            int durationSeconds,
            int finalHp,
            List<Floor> floors
    ) {}

    public record Floor(
            int floor,
            String enemy,
            int turns,
            int hpAfter,
            List<Reward> rewards
    ) {}

    public record Reward(
            List<String> offered,
            String picked
    ) {}

    public record BossFight(
            List<Phase> phases,
            String finishingCard,
            int totalTurns
    ) {}

    public record Phase(
            String phase,
            int turns,
            int damageTaken
    ) {}

    public record Deck(
            int size,
            List<Card> cards
    ) {}

    public record Card(
            String cardType,
            int acquiredFloor
    ) {}
}