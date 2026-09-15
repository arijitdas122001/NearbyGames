package com.gameconnect.game.entity;

import com.gameconnect.game.entity.Game.GameFormat;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class GameFormatConverter implements AttributeConverter<GameFormat, String> {

    @Override
    public String convertToDatabaseColumn(GameFormat attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public GameFormat convertToEntityAttribute(String dbData) {
        return GameFormat.fromDbValue(dbData);
    }
}