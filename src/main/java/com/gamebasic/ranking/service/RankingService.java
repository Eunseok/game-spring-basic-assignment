package com.gamebasic.ranking.service;

import com.gamebasic.ranking.CardType;
import com.gamebasic.ranking.client.RankingClient;
import com.gamebasic.ranking.dto.RankingResponse;
import com.gamebasic.ranking.dto.RankingSource;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final RankingClient rankingClient;

    public RankingResponse getRankings() {
        RankingSource rankingSource = rankingClient.fetch();

        // 1. 순위 대상 필터링(CLEARED 상태, 클리어한 층 = 10)
        List<RankingSource.RankingRecord> records = rankingSource.records()
                .stream()
                .filter(r -> r.run() != null && "CLEARED".equals(r.run().status()) && r.run().clearedFloor() == 10)
                .toList();

        // 2. 정상 기록 검증 및 필터링, 제외한 수 기록(excludedCount)
        List<RankingSource.RankingRecord> filteredRecords = records.stream()
                .filter(RankingService::isValidRecord)
                .collect(Collectors.toList());
        int excludedCount = records.size() - filteredRecords.size();

        // 3. 정렬 , order by 경과시간 ASC, 최종 체력 DESC, ID ASC
        filteredRecords.sort(
                Comparator.comparing((RankingSource.RankingRecord r) -> r.run().durationSeconds())
                        .thenComparing((RankingSource.RankingRecord r) -> r.run().finalHp(), Comparator.reverseOrder())
                        .thenComparing(RankingSource.RankingRecord::id)
        );

        // 4. DISTINCT(플레이어당 최고 기록)
        List<RankingResponse.Entry> entries = createDistinctRankings(filteredRecords);

        return new RankingResponse(
                rankingSource.meta().season().id(),
                rankingSource.meta().totalRecords(),
                excludedCount,
                entries
        );
    }

    private static @NonNull List<RankingResponse.Entry> createDistinctRankings(List<RankingSource.RankingRecord> filteredRecords) {
        Set<String> seenPlayerIds = new HashSet<>();
        List<RankingSource.RankingRecord> distinctRecords = new ArrayList<>();
        for (var record : filteredRecords) {
            if (seenPlayerIds.add(record.player().id())) {
                distinctRecords.add(record);
            }
        }

        // 5. 순위 매기기
        return convertToRankingEntries(distinctRecords);
    }

    private static @NonNull List<RankingResponse.Entry> convertToRankingEntries(List<RankingSource.RankingRecord> distinctRecords) {
        List<RankingResponse.Entry> entries = new ArrayList<>();
        for (int i = 0; i < distinctRecords.size(); i++) {
            var record = distinctRecords.get(i);
            entries.add(new RankingResponse.Entry(
                    i + 1,
                    record.player().name(),
                    record.run().durationSeconds(),
                    record.run().finalHp(),
                    record.bossFight().totalTurns(),
                    record.deck().cards().size()
            ));
        }
        return entries;
    }

    // 랭킹 검증 델리게이트
    private static final List<Predicate<RankingSource.RankingRecord>> BASIC_RULES = List.of(
            RankingService::hasValidDuration,
            RankingService::hasValidFinalHp,
            RankingService::hasValidDeckSize   // null 체크 포함, 이후 규칙의 전제 조건
    );

    private static final List<Predicate<RankingSource.RankingRecord>> DECK_DEPENDENT_RULES = List.of(
            RankingService::hasValidCardTypes,
            RankingService::hasValidAcquiredFloors,
            RankingService::hasValidBossPhaseOrder,
            RankingService::hasConsistentTurnCounts,
            RankingService::hasValidFinishingCard
    );

    private static boolean isValidRecord(RankingSource.RankingRecord r) {
        return BASIC_RULES.stream().allMatch(rule -> rule.test(r))
                && DECK_DEPENDENT_RULES.stream().allMatch(rule -> rule.test(r));
    }

    //----------------------- 정상 기록 검증 규칙 ------------------------------

    private static boolean hasValidDuration(RankingSource.RankingRecord r) {
        return r.run().durationSeconds() >= r.run().clearedFloor() * 30;
    }

    private static boolean hasValidFinalHp(RankingSource.RankingRecord r) {
        return r.run().finalHp() >= 1 && r.run().finalHp() <= 99;
    }

    private static boolean hasValidDeckSize(RankingSource.RankingRecord r) {
        var deck = r.deck();
        if (deck == null || deck.cards() == null) return false;
        int cardCount = deck.cards().size();
        return cardCount >= 9 && cardCount <= 20 && deck.size() == cardCount;
    }

    private static final Set<String> VALID_CARD_TYPE_NAMES = Arrays.stream(CardType.values()).map(Enum::name).collect(Collectors.toSet());
    private static boolean hasValidCardTypes(RankingSource.RankingRecord r) {
        return r.deck().cards().stream().allMatch(
                c -> c.cardType() != null && VALID_CARD_TYPE_NAMES.contains(c.cardType())
        );
    }

    private static boolean hasValidAcquiredFloors(RankingSource.RankingRecord r) {
        return r.deck().cards().stream().allMatch(
                c -> c.acquiredFloor() >= 0 && c.acquiredFloor() <= 9
        );
    }

    private static final List<String> EXPECTED_BOSS_PHASES = List.of("THRONE", "UNBOUND", "ECLIPSE");
    private static boolean hasValidBossPhaseOrder(RankingSource.RankingRecord r) {
        var bossFight = r.bossFight();
        if(bossFight == null || bossFight.phases() == null) return false;
        List<String> actualPhases = bossFight.phases().stream()
                .map(RankingSource.Phase::phase).toList();
        return EXPECTED_BOSS_PHASES.equals(actualPhases);
    }

    private static boolean hasConsistentTurnCounts(RankingSource.RankingRecord r) {
        var phases = r.bossFight().phases();
        if (!phases.stream().allMatch(p -> p.turns() >= 1)) return false;
        int totalTurns = phases.stream().mapToInt(RankingSource.Phase::turns).sum();
        return totalTurns == r.bossFight().totalTurns();
    }

    private static boolean hasValidFinishingCard(RankingSource.RankingRecord r) {
        String finishingCard = r.bossFight().finishingCard();
        return r.deck().cards().stream()
                .anyMatch(c -> c.cardType().equals(finishingCard));
    }
}
