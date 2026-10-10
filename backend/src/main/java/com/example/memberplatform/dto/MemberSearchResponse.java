package com.example.memberplatform.dto;

import java.util.List;

public record MemberSearchResponse(List<MemberResponse> items, int page, boolean hasNext) {
}
