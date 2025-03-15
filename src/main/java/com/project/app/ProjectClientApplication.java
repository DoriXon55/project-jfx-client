package com.project.app;

import com.project.dao.ProjektDAO;
import com.project.dao.ProjektDAOImpl;
import com.project.datasource.DbInitializer;
import com.project.model.Projekt;
import javafx.util.converter.LocalDateTimeStringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class ProjectClientApplication {
	private static final Logger logger = LoggerFactory.getLogger(ProjectClientApplication.class);
	
	public static void main(String[] args) {
		
		DbInitializer.init();
		ProjektDAO projektDAO = new ProjektDAOImpl();
		Projekt projekt = new Projekt("Projekt testowy", "Opis testowy", LocalDate.of(2024,03,15));
		
		
		try {
			projektDAO.setProjekt(projekt);
			logger.info("Id utworzonego pojektu: {}", projekt.getProjektId());
			Integer projektId  = projekt.getProjektId();
			Projekt projekt2 = projektDAO.getProjekt(projektId);
			logger.info("Pobrany projekt - Id: {}, nazwa: {}, opis: {}", projekt2.getProjektId(), projekt2.getNazwa(), projekt2.getOpis());




			// Test getProjekty method
			List<Projekt> projekty = projektDAO.getProjekty(0, 10);
			logger.info("Pobrano {} projektów", projekty.size());
			projekty.forEach(p -> logger.info("Projekt - Id: {}, nazwa: {}", p.getProjektId(), p.getNazwa()));

// Test getProjektyWhereNazwaLike method
			List<Projekt> projektyFiltered = projektDAO.getProjektyWhereNazwaLike("test", 0, 10);
			logger.info("Pobrano {} projektów zawierających 'test' w nazwie", projektyFiltered.size());

// Test getRowsNumber method
			int count = projektDAO.getRowsNumber();
			logger.info("Łączna liczba projektów: {}", count);

// Test deleteProjekt method
			projektDAO.deleteProjekt(projektId);
			logger.info("Usunięto projekt o Id: {}", projektId);

// Verify deletion
			Projekt deletedProjekt = projektDAO.getProjekt(projektId);
			logger.info("Próba pobrania usuniętego projektu: {}", deletedProjekt != null ? "Istnieje" : "Nie istnieje");


			// Test getProjektyWhereDataOddaniaIs method
			LocalDate targetDate = LocalDate.of(2024, 03, 15);
			List<Projekt> projektsByDate = projektDAO.getProjektyWhereDataOddaniaIs(targetDate, 0, 10);
			logger.info("Pobrano {} projektów z datą oddania {}", projektsByDate.size(), targetDate);

// Test getRowsNumberWhereDataOddaniaIs method
			int countByDate = projektDAO.getRowsNumberWhereDataOddaniaIs(targetDate);
			logger.info("Liczba projektów z datą oddania {}: {}", targetDate, countByDate);
			// TODO sprawdzić pozostałe metody
		} catch (RuntimeException e) {
			logger.error("Błąd operacji bazodanowej!", e);
		}
	}

}
