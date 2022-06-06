package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
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
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

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

        assertThat(findMember.getUsername()).isEqualTo("member1");
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
        assertThat(findMember.getUsername()).isEqualTo("member1");
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

        assertThat(findMember.getUsername()).isEqualTo("member1");
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

        assertThat(findMember.getUsername()).isEqualTo("member1");
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


    /*
    * 회원 정렬 순서
    * 1. 회원 나이 내림차순
    * 2. 회원 이름 오름차순
    * 단 2에서 회원 이름이 없으면 마지막에 출력(null last)
   * */
    @Test
    public void sort(){
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작이므로 1로 설정하면 1개를 건너뛴것것
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
   }

    @Test
    public void paging2(){
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //0부터 시작이므로 1로 설정하면 1개를 건너뛴것것
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory //querydsl tuple로 반환
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /*
    * 팀의 이름과 각 팀의 평균 연령을 구해라
    * */
    @Test
    public void group(){
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
//                .having(team.name.eq("teamA"))
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    /*
    * 팀 A에 소속된 모든 회원원
   * */
    @Test
    public void join(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team) //member.team과 Qteam.team을 조인 / 기본적으로 join은 inner join
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /*
    * 연관관계 없는 경우
    * 세타 조인
    * 회원의 이름이 팀 이름과 같은 회원 조회
    * */
    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) //join이 아니라 from절에서 여러 테이블을 가져온뒤 where로 매칭
                .where(member.username.eq(team.name)) // 이 방식으로는 외부조인(left outer, right outer join이 불가능). -> on 을 사용하여 외부 조인 가능
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /*
    * 회원과 팀을 조인하면서 팀이름이 teamA인 팀만 조인, 회원은 모두 조회
    * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
    * */
    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for(Tuple tuple: result){
            System.out.println("tuple = "+ tuple);
        }
    }

    /*
    * 연관관계 없는 엔티티 외부 조인
    * 회원의 이름이 팀 이름과 같은 대상 외부 조인
    * */
    @Test
    public void join_on_no_relation(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for(Tuple tuple: result){
            System.out.println("tuple = "+ tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne(); //member와 team은 LAZY 설정이므로 team이 조회되지 않는다

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());//member의 team이 로딩된상태인지 아직 로딩안된상태인지 확인

        assertThat(loaded).as("페치 조인 미적용").isFalse(); // Lazy상태에 fetch join을 안했으므로 False가 나와야함
    }

    @Test
    public void fetchJoinUse(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() // 그냥 join이랑 같은데 뒤에 fetchJoin()만 붙여주면된다.
                .where(member.username.eq("member1"))
                .fetchOne(); //member와 team은 LAZY 설정이지만 fetch조인했으므로 team이 조회된다

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());//member의 team이 로딩된상태인지 아직 로딩안된상태인지 확인

        assertThat(loaded).as("페치 조인 미적용").isTrue(); // Lazy상태에 fetch join을 했으므로 True가 나와야함
    }
}





















