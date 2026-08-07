package com.deepak.distributed_lovable.workspace_service.repository;

import com.deepak.distributed_lovable.common_lib.enums.ProjectRole;
import com.deepak.distributed_lovable.workspace_service.entity.ProjectMember;
import com.deepak.distributed_lovable.workspace_service.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    List<ProjectMember> findByIdProjectId(Long projectId);

    @Query("""
            Select pm.role from ProjectMember pm
            where pm.id.projectId=:projectId AND pm.id.userId=:userId""")
    Optional<ProjectRole> findRoleByProjectIdAndUserId(@Param("projectId") Long projectId, @Param("userId") Long userId);


    @Query("""
        SELECT count(pm) FROM ProjectMember pm
         where pm.id.userId= :userId and pm.role = 'OWNER'
 """)
    int countProjectOwnedByUser(@Param("userId") Long userId);
}