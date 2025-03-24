package com.project.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class Zadanie {
    private Integer zadanieId;
    private Integer projektId;
    private String nazwa;
    private String opis;
    private Integer kolejnosc;
    private LocalDateTime dataCzasUtworzenia;
    private String status;
    private LocalDate dataRozpoczecia;
    private LocalDate dataZakonczenia;

}