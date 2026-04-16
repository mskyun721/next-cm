package com.ktcloud.kcp.cm.common.exception

import com.ktcloud.kcp.cm.common.errors.SampleErrorCode
import org.springframework.http.HttpStatus

class SampleNotFoundException(id: Long) :
    DefaultException(HttpStatus.NOT_FOUND, SampleErrorCode.SAMPLE_NOT_FOUND, arrayOf(id))
