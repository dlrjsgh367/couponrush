package com.couponrush.member.domain;

import com.couponrush.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Getter
@Table(name = "members")
@Comment("회원")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("회원 PK")
    private Long id;

    @Column(nullable = false, unique = true)
    @Comment("이메일 (로그인 ID, 유니크)")
    private String email;

    @Column(nullable = false)
    @Comment("비밀번호 (BCrypt 해시)")
    private String password;

    @Column(nullable = false, length = 50)
    @Comment("닉네임")
    private String nickname;

    @Builder
    public Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
}
