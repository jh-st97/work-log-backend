package com.worklog.task;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.worklog.common.exception.BusinessException;
import com.worklog.common.exception.ErrorCode;
import com.worklog.member.Member;
import com.worklog.member.MemberRepository;
import com.worklog.project.Project;
import com.worklog.project.ProjectRepository;
import com.worklog.tag.Tag;
import com.worklog.tag.TagRepository;
import com.worklog.tag.dto.TagResponse;
import com.worklog.task.dto.TaskRequest;
import com.worklog.task.dto.TaskResponse;
import com.worklog.task.dto.TaskStatusRequest;
import com.worklog.tasklog.TaskLogRepository;
import com.worklog.worksystem.WorkSystem;
import com.worklog.worksystem.WorkSystemRepository;
import com.worklog.worksystem.dto.WorkSystemResponse;

@Service
public class TaskService {

	private final TaskRepository taskRepository;
	private final TaskTagRepository taskTagRepository;
	private final TaskWorkSystemRepository taskWorkSystemRepository;
	private final MemberRepository memberRepository;
	private final ProjectRepository projectRepository;
	private final TagRepository tagRepository;
	private final WorkSystemRepository workSystemRepository;
	private final TaskResultRepository taskResultRepository;
	private final TaskLogRepository taskLogRepository;

	public TaskService(TaskRepository taskRepository, TaskTagRepository taskTagRepository,
			TaskWorkSystemRepository taskWorkSystemRepository, MemberRepository memberRepository,
			ProjectRepository projectRepository, TagRepository tagRepository,
			WorkSystemRepository workSystemRepository, TaskResultRepository taskResultRepository,
			TaskLogRepository taskLogRepository) {
		this.taskResultRepository = taskResultRepository;
		this.taskLogRepository = taskLogRepository;
		this.taskRepository = taskRepository;
		this.taskTagRepository = taskTagRepository;
		this.taskWorkSystemRepository = taskWorkSystemRepository;
		this.memberRepository = memberRepository;
		this.projectRepository = projectRepository;
		this.tagRepository = tagRepository;
		this.workSystemRepository = workSystemRepository;
	}

	// 내 업무 목록. 상태·우선순위·프로젝트·시스템·태그·마감일 범위·키워드를 조합해서 걸러내고, 페이징한다.
	// null인 필터는 TaskSpecification에서 건너뛴다 — 아무 필터도 안 주면 "보관 안 된 내 업무 전체"가 된다.
	// 읽기 전용 트랜잭션: 지연 로딩(LAZY)인 태그·시스템 이름을 읽는 동안 DB 세션을 열어둔다.
	// 이게 없으면 웹 요청 밖(테스트 등)에서 LazyInitializationException이 난다.
	@Transactional(readOnly = true)
	public Page<TaskResponse> getTasks(Long memberId, TaskStatus status, TaskPriority priority, Long projectId,
			Long systemId, Long tagId, LocalDate dueDateFrom, LocalDate dueDateTo, String keyword,
			boolean archived, Pageable pageable) {
		Specification<Task> spec = TaskSpecification.search(memberId, status, priority, projectId, systemId, tagId,
				dueDateFrom, dueDateTo, keyword, archived);

		Page<Task> tasks = taskRepository.findAll(spec, pageable);

		// 이번 페이지에 나온 업무들의 태그·시스템을 한 번에 모아서 가져온 다음(각 1번씩만 조회),
		// 업무 번호로 묶어(groupingBy) 메모리에서 매칭한다 — 업무마다 따로 조회하지 않아도 됨(N+1 개선).
		List<Long> taskIds = tasks.getContent().stream().map(Task::getId).toList();
		Map<Long, List<TagResponse>> tagsByTaskId = groupTagsByTaskId(taskIds);
		Map<Long, List<WorkSystemResponse>> systemsByTaskId = groupWorkSystemsByTaskId(taskIds);

		return tasks.map(task -> TaskResponse.of(task,
				tagsByTaskId.getOrDefault(task.getId(), List.of()),
				systemsByTaskId.getOrDefault(task.getId(), List.of())));
	}

	@Transactional(readOnly = true)
	public TaskResponse getTask(Long id, Long memberId) {
		Task task = findMyTask(id, memberId);
		return TaskResponse.of(task, getTagResponses(id), getWorkSystemResponses(id));
	}

	@Transactional
	public TaskResponse createTask(Long memberId, TaskRequest request) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

		// 프로젝트가 진짜 내 것인지 확인 (기획서 규칙)
		Project project = findMyProject(request.projectId(), memberId);

		Task task = new Task(member, project, request.title(), request.description(),
				request.priority(), request.dueDate());
		Task saved = taskRepository.save(task);

		// 태그·업무 시스템도 전부 내 것인지 확인한 뒤 연결한다
		List<Tag> tags = resolveTags(request.tagIds(), memberId);
		List<WorkSystem> systems = resolveWorkSystems(request.systemIds(), memberId);
		linkTags(saved, tags);
		linkWorkSystems(saved, systems);

		return TaskResponse.of(saved, tags.stream().map(TagResponse::from).toList(),
				systems.stream().map(WorkSystemResponse::from).toList());
	}

	@Transactional
	public TaskResponse updateTask(Long id, Long memberId, TaskRequest request) {
		Task task = findMyTask(id, memberId);
		Project project = findMyProject(request.projectId(), memberId);

		// 더티 체킹으로 기본 정보만 갱신 (상태는 여기서 안 바꿈)
		task.update(project, request.title(), request.description(), request.priority(), request.dueDate());

		// 태그·업무 시스템 연결은 기존 걸 전부 지우고 요청받은 걸로 새로 만든다.
		// flush()로 삭제를 지금 바로 DB에 반영해야 한다. 안 그러면 Hibernate가
		// INSERT를 DELETE보다 먼저 실행해버려서, 기존과 같은 태그를 다시 넣을 때
		// "이미 있는 키" 에러가 난다 (실제로 겪은 버그, 2026-09-23).
		taskTagRepository.deleteByTaskId(id);
		taskWorkSystemRepository.deleteByTaskId(id);
		taskTagRepository.flush();
		taskWorkSystemRepository.flush();

		List<Tag> tags = resolveTags(request.tagIds(), memberId);
		List<WorkSystem> systems = resolveWorkSystems(request.systemIds(), memberId);
		linkTags(task, tags);
		linkWorkSystems(task, systems);

		return TaskResponse.of(task, tags.stream().map(TagResponse::from).toList(),
				systems.stream().map(WorkSystemResponse::from).toList());
	}

	@Transactional
	public TaskResponse changeStatus(Long id, Long memberId, TaskStatusRequest request) {
		Task task = findMyTask(id, memberId);
		task.changeStatus(request.status());
		return TaskResponse.of(task, getTagResponses(id), getWorkSystemResponses(id));
	}

	@Transactional
	public void archiveTask(Long id, Long memberId) {
		Task task = findMyTask(id, memberId);
		task.archive();
	}

	// 보관한 업무를 영구 삭제한다(되돌릴 수 없음). 보관하지 않은 업무는 실수로 지우지 못하게 막는다.
	// 이 업무에 딸린 성과 항목·진행 메모·태그/시스템 연결은 FK 때문에 먼저 지워야 한다.
	// 진행 메모가 붙어 있던 일일 기록(회고)은 그대로 남는다.
	// 삭제 순서가 어긋나지 않게 자식을 지울 때마다 flush()로 바로 DB에 반영한다.
	@Transactional
	public void deleteTaskPermanently(Long id, Long memberId) {
		Task task = findMyTask(id, memberId);

		if (!task.isArchived()) {
			throw new BusinessException(ErrorCode.NOT_ARCHIVED);
		}

		taskResultRepository.deleteByTaskId(id);
		taskLogRepository.deleteByTaskId(id);
		taskTagRepository.deleteByTaskId(id);
		taskWorkSystemRepository.deleteByTaskId(id);
		taskRepository.flush();

		taskRepository.delete(task);
	}

	// 보관한 업무를 되살린다. 속한 프로젝트가 아직 보관 중이면 되살려도 목록에 안 보이는
	// 이상한 상태가 되므로, 막고 프로젝트부터 복구하라고 안내한다.
	@Transactional
	public TaskResponse restoreTask(Long id, Long memberId) {
		Task task = findMyTask(id, memberId);

		if (task.getProject().isArchived()) {
			throw new BusinessException(ErrorCode.PROJECT_ARCHIVED);
		}

		task.restore();
		return TaskResponse.of(task, getTagResponses(id), getWorkSystemResponses(id));
	}

	// 업무를 찾고, 진짜 내 것인지 확인 (Project와 같은 패턴: 없으면 404, 남의 것이면 403)
	private Task findMyTask(Long id, Long memberId) {
		Task task = taskRepository.findById(id)
				.orElseThrow(() -> new BusinessException(ErrorCode.TASK_NOT_FOUND));

		if (!task.getMember().getId().equals(memberId)) {
			throw new BusinessException(ErrorCode.TASK_ACCESS_DENIED);
		}

		return task;
	}

	private Project findMyProject(Long projectId, Long memberId) {
		Project project = projectRepository.findById(projectId)
				.orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));

		if (!project.getMember().getId().equals(memberId)) {
			throw new BusinessException(ErrorCode.PROJECT_ACCESS_DENIED);
		}

		// 보관 중인 프로젝트에는 업무를 새로 만들거나 옮겨 넣을 수 없다
		if (project.isArchived()) {
			throw new BusinessException(ErrorCode.PROJECT_ARCHIVED);
		}

		return project;
	}

	// 요청받은 태그 번호들이 전부 내 태그인지 확인하면서 엔티티로 바꾼다
	private List<Tag> resolveTags(List<Long> tagIds, Long memberId) {
		if (tagIds == null) {
			return List.of();
		}

		return tagIds.stream()
				.map(tagId -> tagRepository.findByIdAndMemberId(tagId, memberId)
						.orElseThrow(() -> new BusinessException(ErrorCode.TAG_NOT_FOUND)))
				.toList();
	}

	private List<WorkSystem> resolveWorkSystems(List<Long> systemIds, Long memberId) {
		if (systemIds == null) {
			return List.of();
		}

		return systemIds.stream()
				.map(systemId -> workSystemRepository.findByIdAndMemberId(systemId, memberId)
						.orElseThrow(() -> new BusinessException(ErrorCode.WORK_SYSTEM_NOT_FOUND)))
				.toList();
	}

	private void linkTags(Task task, List<Tag> tags) {
		tags.forEach(tag -> taskTagRepository.save(new TaskTag(task, tag)));
	}

	private void linkWorkSystems(Task task, List<WorkSystem> systems) {
		systems.forEach(system -> taskWorkSystemRepository.save(new TaskWorkSystem(task, system)));
	}

	private List<TagResponse> getTagResponses(Long taskId) {
		return taskTagRepository.findByTaskId(taskId).stream()
				.map(taskTag -> TagResponse.from(taskTag.getTag()))
				.toList();
	}

	private List<WorkSystemResponse> getWorkSystemResponses(Long taskId) {
		return taskWorkSystemRepository.findByTaskId(taskId).stream()
				.map(taskWorkSystem -> WorkSystemResponse.from(taskWorkSystem.getWorkSystem()))
				.toList();
	}

	// taskIds가 비어 있으면 쿼리를 아예 안 날린다 (빈 목록 조회 시 불필요한 SQL 방지)
	private Map<Long, List<TagResponse>> groupTagsByTaskId(List<Long> taskIds) {
		if (taskIds.isEmpty()) {
			return Map.of();
		}

		return taskTagRepository.findByTaskIdIn(taskIds).stream()
				.collect(Collectors.groupingBy(
						taskTag -> taskTag.getTask().getId(),
						Collectors.mapping(taskTag -> TagResponse.from(taskTag.getTag()), Collectors.toList())));
	}

	private Map<Long, List<WorkSystemResponse>> groupWorkSystemsByTaskId(List<Long> taskIds) {
		if (taskIds.isEmpty()) {
			return Map.of();
		}

		return taskWorkSystemRepository.findByTaskIdIn(taskIds).stream()
				.collect(Collectors.groupingBy(
						taskWorkSystem -> taskWorkSystem.getTask().getId(),
						Collectors.mapping(taskWorkSystem -> WorkSystemResponse.from(taskWorkSystem.getWorkSystem()),
								Collectors.toList())));
	}

}
