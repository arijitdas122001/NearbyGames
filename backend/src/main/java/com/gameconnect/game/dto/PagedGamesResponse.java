package com.gameconnect.game.dto;

import java.util.List;

public record PagedGamesResponse(
        List<GameSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}