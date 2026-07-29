package com.survivaldiary.global.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 페이징 응답 규약 (전 도메인 공통).
 *
 * 요청 파라미터 규약: ?page=0&size=20&sort=createdAt,desc
 *   - page: 0부터 시작 / size: 기본 20, 최대 100 / sort: {필드},{asc|desc}
 *
 * 사용 예: PageResponse.from(postRepository.findAll(pageable).map(PostDto::from))
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
