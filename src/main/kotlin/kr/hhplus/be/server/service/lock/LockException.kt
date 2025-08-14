package kr.hhplus.be.server.service.lock

class LockAcquisitionTimeoutException(val failedKey: String)
    : RuntimeException("Lock acquire timeout: key=$failedKey")

class LockOrderingViolationException(message: String) : RuntimeException(message)

class LockStateCorruptedException(message: String) : RuntimeException(message)

class LockFairnessViolationException(message: String) : RuntimeException(message)