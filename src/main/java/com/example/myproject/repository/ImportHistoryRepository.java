package com.example.myproject.repository;

import com.example.myproject.domain.ImportHistory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ImportHistoryRepository extends JpaRepository<ImportHistory, Long> {
    @Query(
        "SELECT ih FROM ImportHistory ih WHERE LOWER(ih.bulkId) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(ih.status) LIKE LOWER(CONCAT('%', :search, '%'))"
    )
    Page<ImportHistory> searchImportHistory(@Param("search") String search, Pageable pageable);

    @Modifying
    @Query(value = "DELETE FROM import_history", nativeQuery = true)
    void deleteAllImports();

    @Query(
        "SELECT ih FROM ImportHistory ih WHERE ih.user_login = :userLogin AND (LOWER(ih.bulkId) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(ih.status) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY ih.importDate DESC"
    )
    Page<ImportHistory> findByUserLoginAndSearch(@Param("userLogin") String userLogin, @Param("search") String search, Pageable pageable);

    @Query("SELECT ih FROM ImportHistory ih WHERE ih.user_login = :userLogin ORDER BY ih.importDate DESC")
    Page<ImportHistory> findByUserLogin(@Param("userLogin") String userLogin, Pageable pageable);

    boolean existsByBulkId(@Param("bulkId") String bulkId);
    Optional<ImportHistory> findByBulkId(String bulkId);
}
