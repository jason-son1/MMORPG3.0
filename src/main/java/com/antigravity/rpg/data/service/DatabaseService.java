package com.antigravity.rpg.data.service;

import com.antigravity.rpg.api.service.Service;
import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseService extends Service {
    /**
     * Gets a connection from the pool.
     * Use within try-with-resources.
     */
    Connection getConnection() throws SQLException;
}
