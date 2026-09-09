package com.gamebasic.runcard.repository;

import com.gamebasic.game.entity.Game;
import com.gamebasic.runcard.dto.DeckCount;
import com.gamebasic.runcard.entity.RunCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RunCardRepository extends JpaRepository<RunCard, Long> {
    List<RunCard> findAllByGameOrderByIdAsc(Game game);

    void deleteAllByGame(Game game);

    @Query("SELECT new com.gamebasic.runcard.dto.DeckCount(r.game.id, COUNT(r)) " +
            "FROM RunCard r " +
            "WHERE r.game IN :games " +
            "GROUP BY r.game")
     List<DeckCount> countByGames(List<Game> games);
}
