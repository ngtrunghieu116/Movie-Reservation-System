package com.moviebooking.dto.req;

import com.moviebooking.model.enums.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateArticleRequest {

    @NotBlank(message = "Tiêu đề bài viết không được để trống!")
    @Size(max = 255, message = "Tiêu đề bài viết tối đa 255 ký tự!")
    private String title;

    @NotBlank(message = "Mô tả ngắn không được để trống!")
    @Size(max = 500, message = "Mô tả ngắn tối đa 500 ký tự!")
    private String shortDescription;

    @NotBlank(message = "Nội dung bài viết không được để trống!")
    private String content;

    private ArticleStatus status;
}
