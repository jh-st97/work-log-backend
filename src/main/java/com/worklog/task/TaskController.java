package com.worklog.task;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.worklog.task.dto.TaskRequest;
import com.worklog.task.dto.TaskResponse;
import com.worklog.task.dto.TaskStatusRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

	private final TaskService taskService;

	public TaskController(TaskService taskService) {
		this.taskService = taskService;
	}

	// GET /api/tasks : 내 업무 목록 (필터, 페이징, 정렬)
	// 필터는 전부 선택 사항이다 — 아무것도 안 주면 보관 안 된 내 업무 전체를 최신순으로 보여준다.
	@GetMapping
	public Page<TaskResponse> getTasks(@AuthenticationPrincipal Long memberId,
			@RequestParam(required = false) TaskStatus status,
			@RequestParam(required = false) TaskPriority priority,
			@RequestParam(required = false) Long projectId,
			@RequestParam(required = false) Long systemId,
			@RequestParam(required = false) Long tagId,
			@RequestParam(required = false) LocalDate dueDateFrom,
			@RequestParam(required = false) LocalDate dueDateTo,
			@RequestParam(required = false) String keyword,
			// true면 보관한 업무만 보여준다(보관함). 안 주면 보관 안 된 업무.
			@RequestParam(defaultValue = "false") boolean archived,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return taskService.getTasks(memberId, status, priority, projectId, systemId, tagId, dueDateFrom, dueDateTo,
				keyword, archived, pageable);
	}

	// GET /api/tasks/{id} : 업무 상세
	@GetMapping("/{id}")
	public TaskResponse getTask(@PathVariable Long id, @AuthenticationPrincipal Long memberId) {
		return taskService.getTask(id, memberId);
	}

	// POST /api/tasks : 업무 등록
	@PostMapping
	public ResponseEntity<TaskResponse> createTask(@AuthenticationPrincipal Long memberId,
			@Valid @RequestBody TaskRequest request) {
		TaskResponse response = taskService.createTask(memberId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// PATCH /api/tasks/{id} : 업무 수정 (상태는 여기서 안 바꿈)
	@PatchMapping("/{id}")
	public TaskResponse updateTask(@PathVariable Long id, @AuthenticationPrincipal Long memberId,
			@Valid @RequestBody TaskRequest request) {
		return taskService.updateTask(id, memberId, request);
	}

	// DELETE /api/tasks/{id}/permanent : 보관한 업무를 영구 삭제 (성과·진행 메모도 함께, 되돌릴 수 없음)
	@DeleteMapping("/{id}/permanent")
	public ResponseEntity<Void> deleteTaskPermanently(@PathVariable Long id,
			@AuthenticationPrincipal Long memberId) {
		taskService.deleteTaskPermanently(id, memberId);
		return ResponseEntity.noContent().build();
	}

	// POST /api/tasks/{id}/restore : 보관한 업무 복구
	@PostMapping("/{id}/restore")
	public TaskResponse restoreTask(@PathVariable Long id, @AuthenticationPrincipal Long memberId) {
		return taskService.restoreTask(id, memberId);
	}

	// PATCH /api/tasks/{id}/status : 상태만 따로 변경
	@PatchMapping("/{id}/status")
	public TaskResponse changeStatus(@PathVariable Long id, @AuthenticationPrincipal Long memberId,
			@Valid @RequestBody TaskStatusRequest request) {
		return taskService.changeStatus(id, memberId, request);
	}

	// DELETE /api/tasks/{id} : 실제 삭제가 아니라 보관 처리
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> archiveTask(@PathVariable Long id, @AuthenticationPrincipal Long memberId) {
		taskService.archiveTask(id, memberId);
		return ResponseEntity.noContent().build();
	}

}
