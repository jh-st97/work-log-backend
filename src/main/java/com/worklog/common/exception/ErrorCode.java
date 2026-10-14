package com.worklog.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	
	EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."), 
	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
	PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 프로젝트입니다."),
	PROJECT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 프로젝트에 접근할 권한이 없습니다."),
	TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 태그입니다."),
	TAG_ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 태그에 접근할 권한이 없습니다."),
	TAG_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 태그입니다."),
	WORK_SYSTEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 업무 시스템입니다."),
	WORK_SYSTEM_ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 업무 시스템에 접근할 권한이 없습니다."),
	WORK_SYSTEM_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 업무 시스템입니다."),
	TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 업무입니다."),
	TASK_ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 업무에 접근할 권한이 없습니다."),
	TASK_RESULT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 성과 항목입니다."),
	DAILY_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 일일 기록입니다."),
	TASK_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 진행 메모입니다."),
	TASK_LOG_ACCESS_DENIED(HttpStatus.FORBIDDEN, "이 진행 메모에 접근할 권한이 없습니다."),
	PROJECT_ARCHIVED(HttpStatus.CONFLICT, "프로젝트가 보관 중입니다. 프로젝트를 먼저 복구해 주세요."),
	NOT_ARCHIVED(HttpStatus.CONFLICT, "보관한 항목만 영구 삭제할 수 있습니다. 먼저 보관해 주세요."),
	PROJECT_HAS_TASKS(HttpStatus.CONFLICT, "프로젝트에 업무가 남아 있어 영구 삭제할 수 없습니다. 업무를 먼저 영구 삭제해 주세요."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
	
	private final HttpStatus status;
	private final String message;
	
	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}
	
	public HttpStatus getStatus() {
		return status;
	}
	
	public String getMessage() {
		return message;
	}

}
