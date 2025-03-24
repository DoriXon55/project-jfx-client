package com.project.dao;

import com.project.datasource.DataSource;
import com.project.model.Zadanie;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ZadanieDAOImpl implements ZadanieDAO {
    @Override
    public Zadanie getZadanie(Integer zadanieId) {
        String query = "SELECT * FROM zadanie WHERE zadanie_id = ?";
        try (Connection connection = DataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, zadanieId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToZadanie(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void setZadanie(Zadanie zadanie) {
        boolean isInsert = zadanie.getZadanieId() == null;

        try (Connection connection = DataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                // Jeśli to nowe zadanie, ustaw jego kolejność na największą wartość + 1
                // ponieważ był tutaj problem z unique_kolejnosc
                if (isInsert) {
                    if (zadanie.getKolejnosc() == 0) {
                        try (PreparedStatement stmt = connection.prepareStatement(
                                "SELECT COALESCE(MAX(kolejnosc), 0) + 1 FROM zadanie WHERE projekt_id = ?")) {
                            stmt.setInt(1, zadanie.getProjektId());
                            ResultSet rs = stmt.executeQuery();
                            if (rs.next()) {
                                zadanie.setKolejnosc(rs.getInt(1));
                            } else {
                                zadanie.setKolejnosc(1);
                            }
                        }
                    }
                }

                String query = isInsert
                        ? "INSERT INTO zadanie(projekt_id, nazwa, opis, kolejnosc, status, data_rozpoczecia, data_zakonczenia) VALUES (?,?,?,?,?,?,?)"
                        : "UPDATE zadanie SET projekt_id = ?, nazwa = ?, opis = ?, kolejnosc = ?, status = ?, data_rozpoczecia = ?, data_zakonczenia = ? WHERE zadanie_id = ?";

                try (PreparedStatement preparedStatement = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setInt(1, zadanie.getProjektId());
                    preparedStatement.setString(2, zadanie.getNazwa());
                    preparedStatement.setString(3, zadanie.getOpis());
                    preparedStatement.setInt(4, zadanie.getKolejnosc());

                    if (zadanie.getStatus() != null) {
                        preparedStatement.setString(5, zadanie.getStatus());
                    } else {
                        preparedStatement.setNull(5, Types.VARCHAR);
                    }

                    if (zadanie.getDataRozpoczecia() != null) {
                        preparedStatement.setDate(6, java.sql.Date.valueOf(zadanie.getDataRozpoczecia()));
                    } else {
                        preparedStatement.setNull(6, Types.DATE);
                    }

                    if (zadanie.getDataZakonczenia() != null) {
                        preparedStatement.setDate(7, java.sql.Date.valueOf(zadanie.getDataZakonczenia()));
                    } else {
                        preparedStatement.setNull(7, Types.DATE);
                    }

                    if (!isInsert) {
                        preparedStatement.setInt(8, zadanie.getZadanieId());
                    }

                    int affectedRows = preparedStatement.executeUpdate();

                    if (isInsert && affectedRows > 0) {
                        ResultSet keys = preparedStatement.getGeneratedKeys();
                        if (keys.next()) {
                            zadanie.setZadanieId(keys.getInt(1));
                        }
                        keys.close();
                    }
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new RuntimeException(e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void deleteZadanie(Integer zadanieId) {
        try (Connection connection = DataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Integer projektId = null;
                Integer kolejnosc = null;

                try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT projekt_id, kolejnosc FROM zadanie WHERE zadanie_id = ?")) {
                    stmt.setInt(1, zadanieId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            projektId = rs.getInt("projekt_id");
                            kolejnosc = rs.getInt("kolejnosc");
                        }
                    }
                }

                try (PreparedStatement stmt = connection.prepareStatement(
                        "DELETE FROM zadanie WHERE zadanie_id = ?")) {
                    stmt.setInt(1, zadanieId);
                    stmt.executeUpdate();
                }

                if (projektId != null && kolejnosc != null) {
                    try (PreparedStatement stmt = connection.prepareStatement(
                            "UPDATE zadanie SET kolejnosc = kolejnosc - 1 WHERE projekt_id = ? AND kolejnosc > ?")) {
                        stmt.setInt(1, projektId);
                        stmt.setInt(2, kolejnosc);
                        stmt.executeUpdate();
                    }
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new RuntimeException(e);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Zadanie> getZadaniaByProjekt(Integer projektId) {
        List<Zadanie> zadania = new ArrayList<>();
        String query = "SELECT * FROM zadanie WHERE projekt_id = ? ORDER BY kolejnosc";
        try (Connection connection = DataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, projektId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    zadania.add(mapResultSetToZadanie(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return zadania;
    }

    @Override
    public int getRowsNumberForProjekt(Integer projektId) {
        String query = "SELECT COUNT(*) FROM zadanie WHERE projekt_id = ?";
        try (Connection connection = DataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, projektId);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private Zadanie mapResultSetToZadanie(ResultSet rs) throws SQLException {
        Zadanie zadanie = new Zadanie();
        zadanie.setZadanieId(rs.getInt("zadanie_id"));
        zadanie.setProjektId(rs.getInt("projekt_id"));
        zadanie.setNazwa(rs.getString("nazwa"));
        zadanie.setOpis(rs.getString("opis"));
        zadanie.setKolejnosc(rs.getInt("kolejnosc"));

        String status = rs.getString("status");
        if (!rs.wasNull()) {
            zadanie.setStatus(status);
        }

        java.sql.Date dataRozp = rs.getDate("data_rozpoczecia");
        if (dataRozp != null) {
            zadanie.setDataRozpoczecia(dataRozp.toLocalDate());
        }

        java.sql.Date dataZak = rs.getDate("data_zakonczenia");
        if (dataZak != null) {
            zadanie.setDataZakonczenia(dataZak.toLocalDate());
        }

        zadanie.setDataCzasUtworzenia(rs.getObject("dataczas_utworzenia", java.time.LocalDateTime.class));

        return zadanie;
    }
}