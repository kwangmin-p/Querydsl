package study.querydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {
    //회원명, 팀명, 나이(ageGoe, ageLoe)
    private String username;
    private String teamName;
    private Integer ageGoe; // 값이 null 일 수도 있어서 int가 아닌 Integer로 선언
    private Integer ageLoe;

}
