package com.cirf.dashboard.domain.cases.repository;

import com.cirf.dashboard.domain.cases.entity.AccountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountIdRepository extends JpaRepository<AccountId, Long> {

    /**
     * accountId 문자열로 AccountId 엔티티 조회 (단건)
     */
    Optional<AccountId> findByAccountId(String accountId);  // ✅ 추가!

    /** ✅ 문자열 accountId 목록 기준 존재 개수 (중복 포함) */
    long countByAccountIdIn(List<String> accountIds);

    /** ✅ 문자열 accountId 목록으로 엔티티 조회 */
    List<AccountId> findByAccountIdIn(List<String> accountIds);

    /** (옵션) 중복 제거하여 존재 개수 체크하고 싶을 때 */
    @Query("select count(distinct a.accountId) from AccountId a where a.accountId in :ids")
    long countDistinctByAccountIdIn(@Param("ids") List<String> ids);

    /** (옵션) 단건 존재 여부 */
    boolean existsByAccountId(String accountId);

    /** (옵션) 특정 IncidentCase에 속한 AccountId 목록 조회 */
    List<AccountId> findByIncidentCase_Id(Long caseId);

    /** 🔁 레거시: Long ID 목록을 문자열로 변환해 재사용 (가능하면 새 메서드로 교체 권장) */
    default long countExistingByIds(List<Long> ids) {
        List<String> stringIds = ids.stream().map(String::valueOf).toList();
        return countByAccountIdIn(stringIds);
    }
    List<AccountId> findByIncidentCaseId(Long incidentCaseId);
}
