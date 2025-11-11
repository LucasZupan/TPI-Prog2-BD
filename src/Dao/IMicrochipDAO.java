package Dao;

import Models.Microchip;
import java.sql.Connection;
import java.util.List;

public interface IMicrochipDAO extends GenericDAO<Microchip> {
    Microchip buscarPorCodigo(String codigo) throws Exception;
}
