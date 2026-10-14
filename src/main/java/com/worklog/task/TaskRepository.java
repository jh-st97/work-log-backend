package com.worklog.task;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

// JpaSpecificationExecutor를 추가하면 findAll(Specification, Pageable)처럼
// 동적 조건 + 페이징을 조합한 조회 메서드들이 딸려온다 (업무 목록 필터링에 사용).
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

	// 업무 번호 + 회원 번호로 소유권까지 같이 확인
	Optional<Task> findByIdAndMemberId(Long id, Long memberId);

	// 프로젝트를 영구 삭제하기 전에, 보관한 업무까지 포함해서 업무가 하나라도 남아 있는지 확인
	boolean existsByProjectId(Long projectId);

}
