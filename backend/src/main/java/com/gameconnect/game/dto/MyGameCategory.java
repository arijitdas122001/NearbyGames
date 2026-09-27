package com.gameconnect.game.dto;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.gameconnect.game.entity.Game.GameStatus;

/**
 * User-facing bucket for a game in the "My Games" list. Derived purely from
 * {@link GameStatus} so there is no second source of truth that can disagree
 * with the game completion scheduler.
 */
public enum MyGameCategory {

    UPCOMING("Upcoming", Set.of(GameStatus.OPEN, GameStatus.FULL)),
    IN_PROGRESS("In progress", Set.of(GameStatus.IN_PROGRESS)),
    COMPLETED("Completed", Set.of(GameStatus.COMPLETED)),
    CANCELLED("Cancelled", Set.of(GameStatus.CANCELLED)),
    ALL("All", Set.of(GameStatus.OPEN, GameStatus.FULL, GameStatus.IN_PROGRESS,
            GameStatus.COMPLETED, GameStatus.CANCELLED));

    private final String label;
    private final Set<GameStatus> statuses;

    MyGameCategory(String label, Set<GameStatus> statuses) {
        this.label = label;
        this.statuses = statuses;
    }

    public String label() {
        return label;
    }

    public Collection<GameStatus> statuses() {
        return statuses;
    }

    public static MyGameCategory fromGameStatus(GameStatus status) {
        for (MyGameCategory category : values()) {
            if (category != ALL && category.statuses.contains(status)) {
                return category;
            }
        }
        return ALL;
    }

    /**
     * The categories offered as tabs, in display order. {@link #ALL} is a
     * query convenience only and is never rendered as a tab.
     */
    public static List<MyGameCategory> tabs() {
        return List.of(UPCOMING, IN_PROGRESS, COMPLETED, CANCELLED);
    }
}
