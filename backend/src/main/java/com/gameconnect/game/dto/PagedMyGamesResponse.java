package com.gameconnect.game.dto;

import java.util.List;

public record PagedMyGamesResponse(
        List<MyGameSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
