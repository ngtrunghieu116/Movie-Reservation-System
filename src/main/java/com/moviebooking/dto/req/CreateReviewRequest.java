package com.moviebooking.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReviewRequest {

    @NotNull(message = "ID bộ phim không được để trống!")
    private Long movieId;

    @NotNull(message = "Điểm đánh giá không được để trống!")
    @Min(value = 1, message = "Điểm đánh giá tối thiểu là 1 sao!")
    @Max(value = 5, message = "Điểm đánh giá tối đa là 5 sao!")
    private Integer rating;

    @Size(max = 1000, message = "Nội dung nhận xét tối đa 1000 ký tự!")
    private String comment;
}
