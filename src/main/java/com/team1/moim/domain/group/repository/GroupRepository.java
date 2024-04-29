package com.team1.moim.domain.group.repository;

import com.team1.moim.domain.group.entity.Group;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    @Query("SELECT g FROM Group g JOIN FETCH g.groupInfos WHERE g.id = :id AND g.isConfirmed = :isConfirmed AND g.isDeleted = :isDeleted")
    Optional<Group> findByIsConfirmedAndIsDeletedAndId(
            @Param("isConfirmed") String isConfirmed,
            @Param("isDeleted") String isDeleted,
            @Param("id") Long id);

    @Query("SELECT g FROM Group g JOIN g.groupInfos gi WHERE gi.member = :memberId")
    Page<Group> findAllByMemberId(@Param("memberId") Long memberId, Pageable pageable);
    List<Group> findByIsConfirmed(String isConfirmed);
    List<Group> findByIsConfirmedAndIsDeleted(String isConfirmed, String isDeleted);

    List<Group> findByMemberId(Long memberId);


}
