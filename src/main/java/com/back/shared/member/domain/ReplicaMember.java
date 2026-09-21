package com.back.shared.member.domain;

import com.back.shared.member.dto.MemberDto;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@NoArgsConstructor
public abstract class ReplicaMember extends BaseMember {
    @Id
    private int id;
    private LocalDateTime createDate;
    private LocalDateTime modifyDate;

    public ReplicaMember(
            int id,
            LocalDateTime createDate,
            LocalDateTime modifyDate,
            String username,
            String password,
            String nickname,
            int activityScore
    ) {
        super(username, password, nickname, activityScore);
        this.id = id;
        this.createDate = createDate;
        this.modifyDate = modifyDate;
    }

    public void syncFrom(MemberDto member) {
        setUsername(member.getUsername());
        setNickname(member.getNickname());
        setActivityScore(member.getActivityScore());
        this.createDate = member.getCreateDate();
        this.modifyDate = member.getModifyDate();
    }
}