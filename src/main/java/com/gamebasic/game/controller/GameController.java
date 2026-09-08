package com.gamebasic.game.controller;

import com.gamebasic.game.dto.*;
import com.gamebasic.game.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    @GetMapping("/games")
    public ResponseEntity<List<GameSummaryResponse>> getGames() {
        return ResponseEntity.ok(gameService.getGames());
    }

    @PostMapping("/games")
    public ResponseEntity<GameDetailResponse> createGame(@Valid @RequestBody CreateRequest request) {
        GameDetailResponse created = gameService.createGame(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/games/{gameId}")
    public ResponseEntity<GameDetailResponse> getGame(@PathVariable Long gameId) {
        return ResponseEntity.ok(gameService.getGame(gameId));
    }

     @PutMapping("/games/{gameId}/progress")
     public ResponseEntity<?> updateProgress(
         @PathVariable Long gameId,
         @Valid @RequestBody ProgressRequest request
     ) {
         GameDetailResponse saved = gameService.updateProgress(gameId, request);
         return ResponseEntity.status(HttpStatus.OK).body(saved);
     }

    @PatchMapping("/games/{gameId}")
    public ResponseEntity<Void> renameGame(
            @PathVariable Long gameId,
            @Valid @RequestBody RenameRequest request
    ) {
        gameService.renameGame(gameId, request);
        return ResponseEntity.noContent().build();
    }
}
