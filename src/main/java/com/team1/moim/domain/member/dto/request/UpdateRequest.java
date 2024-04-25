package com.team1.moim.domain.member.dto.request;

import com.team1.moim.domain.member.entity.Member;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateRequest {
    private String nickname;
    private MultipartFile profileImage;
}
