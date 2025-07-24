package kr.hhplus.be.server.service.user.exception

import kr.hhplus.be.server.service.common.exception.ResourceNotFoundException

class UserNotFoundException : ResourceNotFoundException("회원이 존재하지 않습니다.")
