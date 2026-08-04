package com.moviebooking.dto.req;

import com.moviebooking.model.enums.AgeRating;
import com.moviebooking.model.enums.MovieStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Setter
public class MovieRequest {

    @NotBlank(message = "Tên phim không được để trống")
    @Size(max = 200, message = "Tên phim không được vượt quá 200 ký tự")
    private String title;

    private String titleEn;

    @NotBlank(message = "Mô tả phim không được để trống")
    private String description;

    @NotBlank(message = "Tên đạo diễn không được để trống")
    private String director;

    @NotBlank(message = "Danh sách diễn viên không được để trống")
    private String actors;

    @NotNull(message = "Thời lượng phim không được để trống")
    @Min(value = 1, message = "Thời lượng phim phải lớn hơn 0 phút")
    private Integer duration;

    @NotNull(message = "Ngày khởi chiếu không được để trống")
    private LocalDate releaseDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate endDate;

    private String trailerUrl;

    @NotNull(message = "Độ tuổi quy định không được để trống")
    private AgeRating ageRating;

    @NotBlank(message = "Ngôn ngữ không được để trống")
    private String language;

    private String subtitle;

    @NotNull(message = "Trạng thái phim không được để trống")
    private MovieStatus status;

    @NotEmpty(message = "Phim phải thuộc ít nhất một thể loại")
    private Set<Long> genreIds;
}
