package com.cirf.dashboard.domain.cases.repository;

import com.cirf.dashboard.domain.cases.entity.IncidentCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * IncidentCase 엔티티 Repository
 */
@Repository
public interface IncidentCaseRepository extends JpaRepository<IncidentCase, Long> {

    /**
     * 사용자 ID로 사례 목록 조회
     *
     * @param userId 사용자 ID
     * @return 사례 목록
     */
    List<IncidentCase> findByUserId(Long userId);

    /**
     * 사례 이름으로 사례 조회
     *
     * @param caseName 사례 이름
     * @return 사례 Optional
     */
    Optional<IncidentCase> findByCaseName(String caseName);

    /**
     * 사례 이름 중복 체크
     *
     * @param caseName 확인할 사례 이름
     * @return 존재 여부
     */
    boolean existsByCaseName(String caseName);

    /**
     * 특정 사용자의 사례 목록 조회 (페이징)
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 페이징된 사례 목록
     */
    Page<IncidentCase> findByUserId(Long userId, Pageable pageable);

}