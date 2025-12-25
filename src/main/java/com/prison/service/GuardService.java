package com.prison.service;

import com.prison.dao.GuardDao;
import com.prison.model.Guard;

import java.util.List;

public class GuardService {

    private final GuardDao guardDao = new GuardDao();

    public int addGuardAndReturnId(Guard guard) {
        return guardDao.saveAndReturnId(guard);
    }


    public List<Guard> getAllGuards() {
        return guardDao.findAll();
    }

    public void deleteGuard(int id) {
        guardDao.delete(id);
    }
}
