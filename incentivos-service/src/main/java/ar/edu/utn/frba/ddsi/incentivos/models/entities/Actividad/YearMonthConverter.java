package ar.edu.utn.frba.ddsi.incentivos.models.entities.Actividad;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.YearMonth;

@Converter
public class YearMonthConverter implements AttributeConverter<YearMonth, String> {
    // se encarga de traducir el tipo YearMonth de Java a un formato que mysql entienda (y viceversa)
    // se utiliza en ActividadMensual
    @Override
    public String convertToDatabaseColumn(YearMonth attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public YearMonth convertToEntityAttribute(String value) {
        return value == null ? null : YearMonth.parse(value);
    }
}
