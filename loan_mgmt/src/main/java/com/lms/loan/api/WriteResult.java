package com.lms.loan.api;

public record WriteResult<T>(T body, boolean created) {
}
