package org.example.guessinggame.repositories;

import org.example.guessinggame.entities.GameResult;
import org.example.guessinggame.entities.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {

    List<GameResult> findByPlayer(Player player);
}