package com.gameconnect.game.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gameconnect.game.entity.Game;

public interface GameRepository extends JpaRepository<Game, UUID> {
}