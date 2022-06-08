package study.querydsl.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findByUsername(String username); // spring data jpa 가 메소드명으로 자동으로 jpql 을 생성. select m from Member m where m.username = ?
}
