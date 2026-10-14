package com.worklog.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.worklog.common.exception.BusinessException;
import com.worklog.common.exception.ErrorCode;
import com.worklog.member.Member;
import com.worklog.member.MemberRepository;
import com.worklog.project.dto.ProjectRequest;
import com.worklog.project.dto.ProjectResponse;
import com.worklog.task.TaskRepository;

@Service
public class ProjectService {

	private final ProjectRepository projectRepository;
	private final MemberRepository memberRepository;
	private final TaskRepository taskRepository;

	public ProjectService(ProjectRepository projectRepository, MemberRepository memberRepository,
			TaskRepository taskRepository) {
		this.projectRepository = projectRepository;
		this.memberRepository = memberRepository;
		this.taskRepository = taskRepository;
	}

	// 내 프로젝트 목록 조회. archived가 false면 보관 안 된 것, true면 보관한 것(보관함)만.
	public Page<ProjectResponse> getProjects(Long memberId, boolean archived, Pageable pageable) {
		Page<Project> projects = archived
				? projectRepository.findByMemberIdAndArchivedAtIsNotNull(memberId, pageable)
				: projectRepository.findByMemberIdAndArchivedAtIsNull(memberId, pageable);

		return projects.map(ProjectResponse::from); // Project 엔티티를 ProjectResponse로 하나씩 변환
	}

	// 프로젝트 상세 조회 (내 것인지 확인 포함)
	public ProjectResponse getProject(Long id, Long memberId) {
		Project project = findMyProject(id, memberId);
		return ProjectResponse.from(project);
	}

	// 프로젝트 등록
	@Transactional
	public ProjectResponse createProject(Long memberId, ProjectRequest request) {
		// 토큰에 들어있는 회원 번호로 실제 회원을 찾는다 (없으면 404)
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		Project project = new Project(member, request.name(), request.description(),
				request.startDate(), request.endDate());

		Project saved = projectRepository.save(project);
		return ProjectResponse.from(saved);
	}

	// 프로젝트 수정
	@Transactional
	public ProjectResponse updateProject(Long id, Long memberId, ProjectRequest request) {
		Project project = findMyProject(id, memberId);
		// save()를 따로 부르지 않아도, 트랜잭션이 끝날 때 바뀐 값이 자동으로 UPDATE된다 (더티 체킹)
		project.update(request.name(), request.description(), request.startDate(), request.endDate());
		return ProjectResponse.from(project);
	}

	// 프로젝트 보관 처리 (진짜로 삭제하지 않음)
	@Transactional
	public void archiveProject(Long id, Long memberId) {
		Project project = findMyProject(id, memberId);
		project.archive();
	}

	// 보관한 프로젝트 복구. 하위 업무는 업무 목록에서 숨겨져 있었을 뿐(업무 자체의 보관 여부는 그대로)이라
	// 프로젝트만 되살리면 같이 다시 보인다. 업무를 직접 보관했던 것은 보관 상태로 남는다.
	@Transactional
	public ProjectResponse restoreProject(Long id, Long memberId) {
		Project project = findMyProject(id, memberId);
		project.restore();
		return ProjectResponse.from(project);
	}

	// 보관한 프로젝트를 영구 삭제한다(되돌릴 수 없음). 보관하지 않은 프로젝트는 막는다.
	// 그 프로젝트에 업무가 하나라도 남아 있으면(보관한 업무 포함) 업무와 이력서 재료가 한꺼번에
	// 사라지는 사고를 막으려고 삭제를 거부한다. 업무를 먼저 영구 삭제해야 한다.
	@Transactional
	public void deleteProjectPermanently(Long id, Long memberId) {
		Project project = findMyProject(id, memberId);

		if (!project.isArchived()) {
			throw new BusinessException(ErrorCode.NOT_ARCHIVED);
		}
		if (taskRepository.existsByProjectId(id)) {
			throw new BusinessException(ErrorCode.PROJECT_HAS_TASKS);
		}

		projectRepository.delete(project);
	}

	// 프로젝트를 찾고, 그게 진짜 내 것인지까지 확인하는 공통 로직
	// (상세 조회, 수정, 보관 처리에서 다 같이 쓴다)
	private Project findMyProject(Long id, Long memberId) {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND)); // 없으면 404

		if (!project.getMember().getId().equals(memberId)) {
			throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED); // 남의 것이면 403
		}

		return project;
	}

}
