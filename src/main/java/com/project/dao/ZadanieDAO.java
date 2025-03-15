package com.project.dao;

import com.project.model.Zadanie;

import java.util.List;

public interface ZadanieDAO {
    Zadanie getZadanie(Integer zadanieId);

    void setZadanie(Zadanie zadanie);

    void deleteZadanie(Integer zadanieId);

    List<Zadanie> getZadaniaByProjekt(Integer projektId);

    int getRowsNumberForProjekt(Integer projektId);
}