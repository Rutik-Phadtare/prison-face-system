package com.prison.service;

import com.prison.dao.PrisonerDao;
import com.prison.model.Prisoner;

import java.util.List;

public class PrisonerService {

    private final PrisonerDao prisonerDao = new PrisonerDao();

    public int addPrisonerAndReturnId(Prisoner prisoner) {
        return prisonerDao.saveAndReturnId(prisoner);
    }


    public List<Prisoner> getAllPrisoners() {
        return prisonerDao.findAll();
    }

    public void deletePrisoner(int id) {
        prisonerDao.delete(id);
    }
}
