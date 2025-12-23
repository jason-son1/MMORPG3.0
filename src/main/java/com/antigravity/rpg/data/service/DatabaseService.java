package com.antigravity.rpg.data.service;

import com.antigravity.rpg.api.service.Service;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 데이터베이스 커넥션 관리를 위한 서비스 인터페이스입니다.
 */
public interface DatabaseService extends Service {
    /**
     * 커넥션 풀로부터 커넥션을 획득합니다.
     * 반드시 try-with-resources 구문을 내에서 사용하여 자동으로 반납되도록 해야 합니다.
     */
    Connection getConnection() throws SQLException;
}
