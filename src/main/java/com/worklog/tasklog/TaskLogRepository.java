package com.worklog.tasklog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskLogRepository extends JpaRepository<TaskLog, Long> {

	// 하루 일지 화면에서 그 날짜의 진행 메모를 시간순으로 보여줄 때 씀
	List<TaskLog> findByDailyLogIdOrderByCreatedAtAsc(Long dailyLogId);

	// 업무별 진행 메모 조회 (시간순) — GET /api/tasks/{taskId}/task-logs
	List<TaskLog> findByTaskIdOrderByCreatedAtAsc(Long taskId);

	// 업무를 영구 삭제하기 전에, 그 업무의 진행 메모를 전부 지운다 (일일 기록 자체는 남는다)
	void deleteByTaskId(Long taskId);

}
