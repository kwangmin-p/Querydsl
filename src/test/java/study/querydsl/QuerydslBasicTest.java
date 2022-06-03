package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.util.List;

import static study.querydsl.entity.QMember.member;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory; //JPAQueryFactory는 필드 레벨로 가져가도 된다. Spring framework의 entity manager는 내부적으로 동시성 문제 발생 하지 않도록 설계되어있다.

//    각 테스트 실행 전
    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);
        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL(){
//        member1을 찾아라
        String qlString =
                "select m from Member m " +
                "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

//    1.Querydsl 은 컴파일 시점에 오류를 발견할 수 있다
//    2.파라미터 바인딩
    @Test
    public void startQuerydsl(){
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
//        QMember m = new QMember("m"); //직접 별칭 만들어 사용

        QMember m = member; //Q 타입의 기본 멤버 사용. static 선언되어있으므로 static import도 가능. static import 하여 QMember m 선언도 필요없어지고, QMember.member -> member로 변경되어 코드 간략해짐

       Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1")) //파라미터 바인딩
                .fetchOne();
        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(
                        member.username.eq("member1") //and의 chain 형식
                                .and(member.age.eq(10)) //마찬가지로 or 도 가능
                )
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where( //and는 chain형식을 유지하지않고 , 로 여러 파라미터를 받을 수 있음
                        member.username.eq("member1"),
                        member.age.eq(10) //마찬가지로 or 도 가능
                )
                .fetchOne();

        Assertions.assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        List<Member> fetch = queryFactory
                .select(member)
                .from(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne(); //결과 없으면 null, 두건이상이면 NotUniqueResultException 발생

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();//limit(1).fetch(1) 과 결과가 같다

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults(); //fetchResults는 getTotal(개수), getResult(내용, 컨텐츠)을 제공한다. results.getTotal(); results.getResult(); < 페이징 처리 가능

        long total = queryFactory
                .select(member)
                .fetchCount();

    }
}
