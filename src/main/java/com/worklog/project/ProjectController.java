package com.worklog.project;

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

import com.worklog.project.dto.ProjectRequest;
import com.worklog.project.dto.ProjectResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	// GET /api/projects : 내 프로젝트 목록 (page, size, sort). 기본은 먼저 만든 순서.
	// archived=true면 보관한 프로젝트만 보여준다(보관함).
	@GetMapping
	public Page<ProjectResponse> getProjects(@AuthenticationPrincipal Long memberId,
			@RequestParam(defaultValue = "false") boolean archived,
			@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
		return projectService.getProjects(memberId, archived, pageable);
	}

	// DELETE /api/projects/{id}/permanent : 보관한 프로젝트를 영구 삭제 (되돌릴 수 없음)
	@DeleteMapping("/{id}/permanent")
	public ResponseEntity<Void> deleteProjectPermanently(@PathVariable Long id,
			@AuthenticationPrincipal Long memberId) {
		projectService.deleteProjectPermanently(id, memberId);
		return ResponseEntity.noContent().build();
	}

	// POST /api/projects/{id}/restore : 보관한 프로젝트 복구
	@PostMapping("/{id}/restore")
	public ProjectResponse restoreProject(@PathVariable Long id, @AuthenticationPrincipal Long memberId) {
		return projectService.restoreProject(id, memberId);
	}

	// GET /api/projects/{id} : 프로젝트 상세
	@GetMapping("/{id}")
	public ProjectResponse getProject(@PathVariable Long id, @AuthenticationPrincipal Long memberId) {
		return projectService.getProject(id, memberId);
	}

	// POST /api/projects : 프로젝트 등록
	@PostMapping
	public ResponseEntity<ProjectResponse> createProject(@AuthenticationPrincipal Long memberId,
			@Valid @RequestBody ProjectRequest request) {
		ProjectResponse response = projectService.createProject(memberId, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// PATCH /api/projects/{id} : 프로젝트 수정
	@PatchMapping("/{id}")
	public ProjectResponse updateProject(@PathVariable Long id, @AuthenticationPrincipal Long memberId,
			@Valid @RequestBody ProjectRequest request) {
		return projectService.updateProject(id, memberId, request);
	}

	// DELETE /api/projects/{id} : 실제 삭제가 아니라 보관 처리
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> archiveProject(@PathVariable Long id, @AuthenticationPrincipal Long memberId) {
		projectService.archiveProject(id, memberId);
		return ResponseEntity.noContent().build(); // 204: 성공했지만 돌려줄 내용 없음
	}

}
