package com.gamebasic.game.service;

import com.gamebasic.common.exception.GameFinishedException;
import com.gamebasic.common.exception.GameNotFoundException;
import com.gamebasic.game.dto.*;
import com.gamebasic.game.entity.Game;
import com.gamebasic.game.repository.GameRepository;
import com.gamebasic.runcard.dto.CardResponse;
import com.gamebasic.runcard.dto.DeckCount;
import com.gamebasic.runcard.dto.RunCardRequest;
import com.gamebasic.runcard.entity.RunCard;
import com.gamebasic.runcard.repository.RunCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GameService {

    private final GameRepository gameRepository;
    private final RunCardRepository runCardRepository;

    public GameDetailResponse createGame(CreateRequest request) {
        Game game = gameRepository.save(new Game(request.getPlayerName()));
        saveDeck(game, request.getDeck());
        List<RunCard> cards = runCardRepository.findAllByGameOrderByIdAsc(game);
        List<CardResponse> deck = new ArrayList<>();
        for (RunCard card : cards) {
            deck.add(new CardResponse(card.getId(), card.getCardType(), card.getAcquiredFloor()));
        }
        return new GameDetailResponse(
                game.getId(),
                game.getPlayerName(),
                game.getCurrentHp(),
                game.getCurrentFloor(),
                game.getPhase(),
                game.getStatus(),
                deck
        );
    }

    private void saveDeck(Game game, List<RunCardRequest> deck) {
        List<RunCard> cards = new ArrayList<>();
        for (RunCardRequest card : deck) {
            cards.add(new RunCard(game, card.getCardType(), card.getAcquiredFloor()));
        }
        runCardRepository.saveAll(cards);
    }

    private Game findGame(Long gameId) {
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
    }

    public GameDetailResponse updateProgress(Long gameId, ProgressRequest request) {
        Game game = findGame(gameId);
        if(game.isFinished()) {
            throw new GameFinishedException(gameId);
        }

        game.updateProgress(
                request.getCurrentHp(),
                request.getCurrentFloor(),
                request.getPhase(),
                request.getStatus()
        );
        // 요청의 deck은 저장할 덱 전체이므로 기존 카드를 모두 지우고 요청 순서대로 다시 저장합니다.
        runCardRepository.deleteAllByGame(game);
        saveDeck(game, request.getDeck());
        List<RunCard> cards = runCardRepository.findAllByGameOrderByIdAsc(game);
        List<CardResponse> deck = new ArrayList<>();
        for (RunCard card : cards) {
            deck.add(new CardResponse(card.getId(), card.getCardType(), card.getAcquiredFloor()));
        }
        return new GameDetailResponse(
                game.getId(),
                game.getPlayerName(),
                game.getCurrentHp(),
                game.getCurrentFloor(),
                game.getPhase(),
                game.getStatus(),
                deck
        );
    }

    @Transactional(readOnly = true)
    public List<GameSummaryResponse> getGames() {
        // #1 Game 내림차순 조회
        List<Game> games = gameRepository.findAllGameByOrderByIdDesc();

        // #2 DeckCount 조회
        List<DeckCount> deckCounts = runCardRepository.countByGames(games);

        // #3 Mapping(Id, Game)
        Map<Long, Game> gameMap = games.stream().collect(Collectors.toMap(Game::getId, Function.identity()));

        // #4 응답 DTO 반환
        return deckCounts.stream()
                .map(deckCount -> {
                    Game game = gameMap.get(deckCount.getGameId());
                    return GameSummaryResponse.builder()
                        .id(game.getId())
                        .playerName(game.getPlayerName())
                        .currentFloor(game.getCurrentFloor())
                        .currentHp(game.getCurrentHp())
                        .phase(String.valueOf(game.getPhase()))
                        .status(String.valueOf(game.getStatus()))
                        .deckSize(deckCount.getDeckSize().intValue())
                        .createAt(game.getCreatedAt())
                        .updateAt(game.getUpdateAt())
                        .build();
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public GameDetailResponse getGame(Long gameId) {
        Game game = findGame(gameId);
        List<RunCard> cards = runCardRepository.findAllByGameOrderByIdAsc(game);
        List<CardResponse> deck = new ArrayList<>();
        for (RunCard card : cards) {
            deck.add(new CardResponse(card.getId(), card.getCardType(), card.getAcquiredFloor()));
        }
        return new GameDetailResponse(
                game.getId(),
                game.getPlayerName(),
                game.getCurrentHp(),
                game.getCurrentFloor(),
                game.getPhase(),
                game.getStatus(),
                deck
        );
    }

    public void renameGame(Long gameId, RenameRequest request) {
        Game game = findGame(gameId);
        game.rename(request.getPlayerName());
    }

    public void deleteGame(Long gameId) {
        runCardRepository.deleteAllByGame(findGame(gameId));
        gameRepository.deleteById(gameId);
    }
}
