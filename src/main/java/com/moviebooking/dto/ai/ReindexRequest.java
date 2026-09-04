package com.moviebooking.dto.ai;

import lombok.*;

/**
 * Request body cho API reindex.
 * Cho phép Admin chọn index lại từng loại nguồn dữ liệu.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReindexRequest {

    @Builder.Default
    private boolean reindexMovies = true;

    @Builder.Default
    private boolean reindexReviews = true;

    @Builder.Default
    private boolean reindexArticles = true;

    /** Nếu true, xóa toàn bộ collection trước khi reindex */
    @Builder.Default
    private boolean clearExisting = false;
}
