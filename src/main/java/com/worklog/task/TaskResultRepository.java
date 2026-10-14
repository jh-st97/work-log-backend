package com.worklog.task;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskResultRepository extends JpaRepository<TaskResult, Long> {

	// 이 업무에 달린 성과 항목 전체 조회
	List<TaskResult> findByTaskId(Long taskId);

	// 성과 항목 번호 + 업무 번호로, 진짜 이 업무 것 맞는지 확인
	Optional<TaskResult> findByIdAndTaskId(Long id, Long taskId);

	// 업무를 영구 삭제하기 전에, 그 업무의 성과 항목을 전부 지운다
	void deleteByTaskId(Long taskId);

}
