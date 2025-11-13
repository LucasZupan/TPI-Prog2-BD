package Dao;

import Models.Microchip;

public interface IMicrochipDAO extends GenericDAO<Microchip> {
    Microchip buscarPorCodigo(String codigo) throws Exception;
}
